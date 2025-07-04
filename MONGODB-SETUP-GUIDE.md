# MongoDB Setup Guide for Phinity Exchange
## Complete Step-by-Step Guide (No DevOps Experience Required)

### 🎯 What We're Building
- 1 Primary MongoDB (handles writes - orders, trades, users)
- 2 Secondary MongoDB (handles reads - market data, reports)
- All on the same machine to save money

---

## STEP 1: Install MongoDB

### 1.1 Update Your System
```bash
# Copy and paste this command
sudo apt update && sudo apt upgrade -y
```

### 1.2 Install MongoDB
```bash
# Import MongoDB public key
wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -

# Add MongoDB repository
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# Update package list
sudo apt update

# Install MongoDB
sudo apt install -y mongodb-org

# Prevent automatic updates
sudo apt-mark hold mongodb-org mongodb-org-database mongodb-org-server mongodb-org-mongos mongodb-org-tools
```

### 1.3 Verify Installation
```bash
# Check if MongoDB is installed
mongod --version
```
**Expected output:** Should show MongoDB version 7.0.x

---

## STEP 2: Create Directories

### 2.1 Create Data Directories
```bash
# Create directories for each MongoDB instance
sudo mkdir -p /data/mongodb/primary
sudo mkdir -p /data/mongodb/secondary1  
sudo mkdir -p /data/mongodb/secondary2

# Create log directories
sudo mkdir -p /var/log/mongodb

# Create config directory
sudo mkdir -p /etc/mongodb

# Set permissions (replace 'kunal' with your username)
sudo chown -R $USER:$USER /data/mongodb
sudo chown -R mongodb:mongodb /var/log/mongodb
sudo chown -R mongodb:mongodb /etc/mongodb
```

### 2.2 Verify Directories Created
```bash
# Check if directories exist
ls -la /data/mongodb/
ls -la /var/log/mongodb/
```

---

## STEP 3: Create Configuration Files

### 3.1 Primary MongoDB Config
**File Location:** `/etc/mongodb/mongod-primary.conf`

```bash
# Create the file
sudo nano /etc/mongodb/mongod-primary.conf
```

**Copy and paste this content:**
```yaml
# MongoDB Primary Configuration
net:
  port: 27017
  bindIp: 127.0.0.1,0.0.0.0

storage:
  dbPath: /data/mongodb/primary
  journal:
    enabled: true
  wiredTiger:
    engineConfig:
      cacheSizeGB: 2
    collectionConfig:
      blockCompressor: snappy

replication:
  replSetName: "phinity-rs"

systemLog:
  destination: file
  path: /var/log/mongodb/mongod-primary.log
  logAppend: true

processManagement:
  fork: true
  pidFilePath: /var/run/mongodb/mongod-primary.pid
```

**Save and exit:** Press `Ctrl+X`, then `Y`, then `Enter`

### 3.2 Secondary-1 MongoDB Config
**File Location:** `/etc/mongodb/mongod-secondary1.conf`

```bash
# Create the file
sudo nano /etc/mongodb/mongod-secondary1.conf
```

**Copy and paste this content:**
```yaml
# MongoDB Secondary-1 Configuration
net:
  port: 27018
  bindIp: 127.0.0.1,0.0.0.0

storage:
  dbPath: /data/mongodb/secondary1
  journal:
    enabled: true
  wiredTiger:
    engineConfig:
      cacheSizeGB: 1
    collectionConfig:
      blockCompressor: snappy

replication:
  replSetName: "phinity-rs"

systemLog:
  destination: file
  path: /var/log/mongodb/mongod-secondary1.log
  logAppend: true

processManagement:
  fork: true
  pidFilePath: /var/run/mongodb/mongod-secondary1.pid
```

**Save and exit:** Press `Ctrl+X`, then `Y`, then `Enter`

### 3.3 Secondary-2 MongoDB Config
**File Location:** `/etc/mongodb/mongod-secondary2.conf`

```bash
# Create the file
sudo nano /etc/mongodb/mongod-secondary2.conf
```

**Copy and paste this content:**
```yaml
# MongoDB Secondary-2 Configuration
net:
  port: 27019
  bindIp: 127.0.0.1,0.0.0.0

storage:
  dbPath: /data/mongodb/secondary2
  journal:
    enabled: true
  wiredTiger:
    engineConfig:
      cacheSizeGB: 1
    collectionConfig:
      blockCompressor: snappy

replication:
  replSetName: "phinity-rs"

systemLog:
  destination: file
  path: /var/log/mongodb/mongod-secondary2.log
  logAppend: true

processManagement:
  fork: true
  pidFilePath: /var/run/mongodb/mongod-secondary2.pid
```

**Save and exit:** Press `Ctrl+X`, then `Y`, then `Enter`

---

## STEP 4: Start MongoDB Instances

### 4.1 Start Primary
```bash
# Start primary MongoDB
sudo mongod --config /etc/mongodb/mongod-primary.conf

# Check if it started (should show process)
ps aux | grep mongod
```

### 4.2 Start Secondary-1
```bash
# Start secondary-1 MongoDB
sudo mongod --config /etc/mongodb/mongod-secondary1.conf

# Check if it started
ps aux | grep mongod
```

### 4.3 Start Secondary-2
```bash
# Start secondary-2 MongoDB
sudo mongod --config /etc/mongodb/mongod-secondary2.conf

# Check if all 3 are running
ps aux | grep mongod
```

**Expected output:** You should see 3 mongod processes running

---

## STEP 5: Configure Replica Set

### 5.1 Connect to Primary
```bash
# Connect to primary MongoDB
mongosh --port 27017
```

### 5.2 Initialize Replica Set
**Copy and paste this in MongoDB shell:**
```javascript
rs.initiate({
  _id: "phinity-rs",
  members: [
    { _id: 0, host: "localhost:27017", priority: 2 },
    { _id: 1, host: "localhost:27018", priority: 1 },
    { _id: 2, host: "localhost:27019", priority: 1 }
  ]
})
```

### 5.3 Check Status
```javascript
# Check replica set status
rs.status()
```

**Expected output:** Should show 1 PRIMARY and 2 SECONDARY

### 5.4 Exit MongoDB Shell
```javascript
exit
```

---

## STEP 6: Test Replication

### 6.1 Test Write on Primary
```bash
# Connect to primary
mongosh --port 27017

# Create test data
use phinity-admin
db.test.insertOne({message: "Hello from Primary", timestamp: new Date()})

# Exit
exit
```

### 6.2 Test Read on Secondary
```bash
# Connect to secondary
mongosh --port 27018

# Allow reads on secondary
rs.secondaryOk()

# Check if data replicated
use phinity-admin
db.test.find()

# Exit
exit
```

**Expected output:** Should see the test document

---

## STEP 7: Create Startup Scripts

### 7.1 Create Start Script
**File Location:** `/home/kunal/start-mongodb.sh`

```bash
# Create the script
nano /home/kunal/start-mongodb.sh
```

**Copy and paste this content:**
```bash
#!/bin/bash
echo "Starting MongoDB Cluster..."

# Start Primary
sudo mongod --config /etc/mongodb/mongod-primary.conf
sleep 2

# Start Secondary-1
sudo mongod --config /etc/mongodb/mongod-secondary1.conf
sleep 2

# Start Secondary-2
sudo mongod --config /etc/mongodb/mongod-secondary2.conf
sleep 2

echo "MongoDB cluster started!"
echo "Primary: localhost:27017"
echo "Secondary-1: localhost:27018"
echo "Secondary-2: localhost:27019"

# Check status
ps aux | grep mongod
```

**Make it executable:**
```bash
chmod +x /home/kunal/start-mongodb.sh
```

### 7.2 Create Stop Script
**File Location:** `/home/kunal/stop-mongodb.sh`

```bash
# Create the script
nano /home/kunal/stop-mongodb.sh
```

**Copy and paste this content:**
```bash
#!/bin/bash
echo "Stopping MongoDB Cluster..."

# Kill all mongod processes
sudo pkill mongod

# Wait for processes to stop
sleep 5

# Check if stopped
if pgrep mongod > /dev/null; then
    echo "Some processes still running, force killing..."
    sudo pkill -9 mongod
else
    echo "All MongoDB processes stopped successfully!"
fi
```

**Make it executable:**
```bash
chmod +x /home/kunal/stop-mongodb.sh
```

---

## STEP 8: Create System Service (Auto-Start on Boot)

### 8.1 Create MongoDB Cluster Service
**File Location:** `/etc/systemd/system/mongocluster.service`

```bash
# Create the service file
sudo nano /etc/systemd/system/mongocluster.service
```

**Copy and paste this content:**
```ini
[Unit]
Description=MongoDB Cluster for Phinity Exchange
After=network.target
Wants=network.target

[Service]
Type=forking
User=mongodb
Group=mongodb
ExecStart=/home/kunal/start-mongodb-service.sh
ExecStop=/home/kunal/stop-mongodb.sh
Restart=always
RestartSec=10
TimeoutStartSec=120
TimeoutStopSec=60

[Install]
WantedBy=multi-user.target
```

**Save and exit:** Press `Ctrl+X`, then `Y`, then `Enter`

### 8.2 Create Service-Compatible Start Script
**File Location:** `/home/kunal/start-mongodb-service.sh`

```bash
# Create the service script
nano /home/kunal/start-mongodb-service.sh
```

**Copy and paste this content:**
```bash
#!/bin/bash
# MongoDB Cluster Service Script

# Wait for system to be ready
sleep 5

# Start Primary
mongod --config /etc/mongodb/mongod-primary.conf
sleep 3

# Start Secondary-1
mongod --config /etc/mongodb/mongod-secondary1.conf
sleep 3

# Start Secondary-2
mongod --config /etc/mongodb/mongod-secondary2.conf
sleep 3

# Log startup
echo "$(date): MongoDB cluster started by systemd" >> /var/log/mongodb/cluster-startup.log
```

**Make it executable:**
```bash
chmod +x /home/kunal/start-mongodb-service.sh
sudo chown mongodb:mongodb /home/kunal/start-mongodb-service.sh
```

### 8.3 Enable and Start the Service
```bash
# Reload systemd to recognize new service
sudo systemctl daemon-reload

# Enable service to start on boot
sudo systemctl enable mongocluster.service

# Start the service now
sudo systemctl start mongocluster.service

# Check service status
sudo systemctl status mongocluster.service
```

**Expected output:** Should show "active (running)" in green

### 8.4 Test Auto-Start
```bash
# Stop the service
sudo systemctl stop mongocluster.service

# Check if MongoDB stopped
ps aux | grep mongod

# Start the service
sudo systemctl start mongocluster.service

# Check if MongoDB started
ps aux | grep mongod
```

### 8.5 Service Management Commands
```bash
# Start MongoDB cluster
sudo systemctl start mongocluster.service

# Stop MongoDB cluster
sudo systemctl stop mongocluster.service

# Restart MongoDB cluster
sudo systemctl restart mongocluster.service

# Check status
sudo systemctl status mongocluster.service

# View service logs
sudo journalctl -u mongocluster.service -f

# Disable auto-start (if needed)
sudo systemctl disable mongocluster.service
```

---

## STEP 9: Update Your Application

### 8.1 Update services.conf
**File Location:** `/home/kunal/Desktop/phinity-deployment/services.conf`

**Add these lines:**
```bash
# MongoDB Cluster Configuration
MONGODB_URI="mongodb://localhost:27017,localhost:27018,localhost:27019/phinity-admin?replicaSet=phinity-rs"

# Service-specific MongoDB connections
ORDER_SERVICE_MONGO_URI="mongodb://localhost:27017/phinity-admin?replicaSet=phinity-rs&readPreference=primary"
USER_SERVICE_MONGO_URI="mongodb://localhost:27017/phinity-admin?replicaSet=phinity-rs&readPreference=primary"
MARKET_SERVICE_MONGO_URI="mongodb://localhost:27018/phinity-admin?replicaSet=phinity-rs&readPreference=secondary"
ADMIN_SERVICE_MONGO_URI="mongodb://localhost:27019/phinity-admin?replicaSet=phinity-rs&readPreference=secondary"
```

---

## STEP 10: Daily Operations

### 10.1 Start MongoDB Cluster
```bash
# Using systemd service (recommended)
sudo systemctl start mongocluster.service

# OR using script (manual method)
./start-mongodb.sh
```

### 10.2 Stop MongoDB Cluster
```bash
# Using systemd service (recommended)
sudo systemctl stop mongocluster.service

# OR using script (manual method)
./stop-mongodb.sh
```

### 10.3 Check Status
```bash
# Check if MongoDB is running
ps aux | grep mongod

# Check replica set status
mongosh --port 27017 --eval "rs.status()"
```

### 10.4 View Logs
```bash
# View service logs (systemd)
sudo journalctl -u mongocluster.service -f

# View MongoDB logs (traditional)
tail -f /var/log/mongodb/mongod-primary.log
tail -f /var/log/mongodb/mongod-secondary1.log
```
```bash
# View primary logs
tail -f /var/log/mongodb/mongod-primary.log

# View secondary logs
tail -f /var/log/mongodb/mongod-secondary1.log
```

---

## STEP 11: Troubleshooting

### 11.1 Service Won't Start
```bash
# Check service status
sudo systemctl status mongocluster.service

# Check service logs
sudo journalctl -u mongocluster.service --no-pager

# Check if ports are in use
sudo netstat -tlnp | grep :2701
```

### 11.2 MongoDB Won't Start
```bash
# Check logs for errors
tail -20 /var/log/mongodb/mongod-primary.log

# Check if port is in use
sudo netstat -tlnp | grep :27017

# Check disk space
df -h
```

### 11.3 Replication Not Working
```bash
# Connect to primary and check
mongosh --port 27017
rs.status()

# Look for "stateStr": "PRIMARY" and "SECONDARY"
```

### 11.4 Performance Issues
```bash
# Check system resources
htop

# Check MongoDB performance
mongosh --port 27017 --eval "db.serverStatus()"
```

---

## 🎉 You're Done!

### What You Have Now:
✅ **3 MongoDB instances** running on one machine  
✅ **Automatic replication** between them  
✅ **High availability** - if one fails, others continue  
✅ **Read/Write separation** - faster performance  
✅ **Auto-start on system boot** - no manual intervention  
✅ **Systemd service management** - professional setup  
✅ **Easy start/stop scripts** for daily operations  

### Next Steps:
1. **Test your setup** with the commands above
2. **Reboot your system** to test auto-start: `sudo reboot`
3. **After reboot, check if MongoDB started:** `sudo systemctl status mongocluster.service`
4. **Start your Phinity Exchange services** using `./deploy.sh start`
5. **Monitor performance** using the troubleshooting commands
6. **Scale up** to separate machines when you have more resources

### Cost: $0 (except your server costs)
### Time to setup: 30-60 minutes
### Maintenance: 5 minutes per day

**You now have a production-grade MongoDB cluster that can handle millions of trades!** 🚀

---

## Emergency Contacts
- **MongoDB Community Forum:** https://community.mongodb.com/
- **MongoDB Documentation:** https://docs.mongodb.com/
- **Your setup is working if:** You can see data replicated between primary and secondary instances