#!/bin/bash

echo "=== MySQL Master-Slave Replication Setup ==="

# Master에서 replication 사용자 생성
echo "Step 1: Creating replication user on Master..."
docker exec -i kotlin-practice-mysql-master mysql -uroot -proot1234 <<EOF
CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED BY 'repl1234';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;
EOF

if [ $? -eq 0 ]; then
    echo "✓ Replication user created successfully"
else
    echo "✗ Failed to create replication user"
    exit 1
fi

# Slave 설정
echo "Step 2: Configuring Slave to replicate from Master..."
docker exec -i kotlin-practice-mysql-slave mysql -uroot -proot1234 <<EOF
STOP SLAVE;
CHANGE MASTER TO
    MASTER_HOST='mysql-master',
    MASTER_USER='repl',
    MASTER_PASSWORD='repl1234',
    MASTER_AUTO_POSITION=1;
START SLAVE;
EOF

if [ $? -eq 0 ]; then
    echo "✓ Slave configured successfully"
else
    echo "✗ Failed to configure slave"
    exit 1
fi

# Replication 상태 확인
echo "Step 3: Checking replication status..."
docker exec -i kotlin-practice-mysql-slave mysql -uroot -proot1234 -e "SHOW SLAVE STATUS\G" | grep -E "Slave_IO_Running|Slave_SQL_Running"

echo ""
echo "=== Replication setup completed ==="
echo "Master: localhost:3306"
echo "Slave: localhost:3307"
echo ""
echo "To verify replication:"
echo "  docker exec -i kotlin-practice-mysql-slave mysql -uroot -proot1234 -e 'SHOW SLAVE STATUS\G'"
