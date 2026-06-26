#!/bin/sh

echo "=== LedgerCore Startup ==="
echo "SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"
echo "PORT: $PORT"
echo "=========================="

exec java -jar app.jar
