@echo off
echo Initializing Restaurant Management System Database...
echo.

REM Update config.properties with your MySQL credentials first
mysql -u root -p < database_schema.sql

echo.
echo Database initialization complete!
echo Default admin user: admin / admin123
echo Please change the password after first login!
pause
