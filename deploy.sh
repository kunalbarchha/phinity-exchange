#!/bin/bash

# Colors for professional output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m'

# Configuration - will be set in main function
DEPLOY_DIR=""
PID_DIR=""
LOG_DIR=""
CONFIG_FILE=""

# Service definitions with ports and dependencies
declare -A SERVICES=(
    ["eureka-service"]="8000"
    ["gateway-service"]="8001"
    ["user-service"]="8002"
    ["market-service"]="8003"
    ["admin-service"]="8004"
    ["matching-service"]="8005"
    ["order-service"]="8006"
    ["tradingview-service"]="8007"
    ["wallet-service"]="8008"
    ["websocket-service"]="8009"
)

# Service startup order (dependencies)
STARTUP_ORDER=("eureka-service" "gateway-service" "user-service" "market-service" "admin-service" "matching-service" "order-service" "tradingview-service" "wallet-service" "websocket-service")

# Clear screen and show header
clear
echo -e "${PURPLE}"
echo "╔══════════════════════════════════════════════════════════════════════════════╗"
echo "║                      🚀 PHINITY EXCHANGE DEPLOYER 🚀                        ║"
echo "║                         Service Management System                            ║"
echo "║                              Version 1.0.0                                   ║"
echo "╚══════════════════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo

# Initialize directories
init_directories() {
    mkdir -p "$PID_DIR" "$LOG_DIR"
    if [ ! -f "$CONFIG_FILE" ]; then
        echo "# Phinity Exchange Services Configuration" > "$CONFIG_FILE"
        echo "JAVA_OPTS=\"-Xms512m -Xmx1024m\"" >> "$CONFIG_FILE"
        echo "SPRING_PROFILES_ACTIVE=local" >> "$CONFIG_FILE"
    fi
}

# Load configuration
load_config() {
    if [ -f "$CONFIG_FILE" ]; then
        source "$CONFIG_FILE"
    fi
}

# Get service status
get_service_status() {
    local service=$1
    local pid_file="$PID_DIR/${service}.pid"
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p "$pid" > /dev/null 2>&1; then
            echo "RUNNING"
        else
            echo "STOPPED"
        fi
    else
        echo "STOPPED"
    fi
}

# Get service PID
get_service_pid() {
    local service=$1
    local pid_file="$PID_DIR/${service}.pid"
    
    if [ -f "$pid_file" ]; then
        cat "$pid_file"
    else
        echo "N/A"
    fi
}

# Start a service
start_service() {
    local service=$1
    local port=${SERVICES[$service]}
    local jar_file="$DEPLOY_DIR/${service}-1.0-SNAPSHOT.jar"
    local pid_file="$PID_DIR/${service}.pid"
    local log_file="$LOG_DIR/${service}.log"
    
    if [ ! -f "$jar_file" ]; then
        echo -e "${RED}   ❌ JAR file not found: $jar_file${NC}"
        return 1
    fi
    
    local status=$(get_service_status "$service")
    if [ "$status" = "RUNNING" ]; then
        echo -e "${YELLOW}   ⚠️  Service already running${NC}"
        return 0
    fi
    
    echo -e "${WHITE}   🚀 Starting $service on port $port...${NC}"
    
    # Start the service
    nohup java $JAVA_OPTS \
        -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-local} \
        -Dserver.port=$port \
        -jar "$jar_file" \
        > "$log_file" 2>&1 &
    
    local pid=$!
    echo $pid > "$pid_file"
    
    # Wait a moment and check if service started successfully
    sleep 3
    if ps -p "$pid" > /dev/null 2>&1; then
        echo -e "${GREEN}   ✅ Started successfully (PID: $pid)${NC}"
        return 0
    else
        echo -e "${RED}   ❌ Failed to start${NC}"
        rm -f "$pid_file"
        return 1
    fi
}

# Stop a service
stop_service() {
    local service=$1
    local pid_file="$PID_DIR/${service}.pid"
    
    if [ ! -f "$pid_file" ]; then
        echo -e "${YELLOW}   ⚠️  Service not running${NC}"
        return 0
    fi
    
    local pid=$(cat "$pid_file")
    echo -e "${WHITE}   🛑 Stopping $service (PID: $pid)...${NC}"
    
    if ps -p "$pid" > /dev/null 2>&1; then
        kill "$pid"
        
        # Wait for graceful shutdown
        local count=0
        while ps -p "$pid" > /dev/null 2>&1 && [ $count -lt 30 ]; do
            sleep 1
            count=$((count + 1))
        done
        
        # Force kill if still running
        if ps -p "$pid" > /dev/null 2>&1; then
            echo -e "${YELLOW}   ⚡ Force killing service...${NC}"
            kill -9 "$pid"
            sleep 2
        fi
        
        if ! ps -p "$pid" > /dev/null 2>&1; then
            echo -e "${GREEN}   ✅ Stopped successfully${NC}"
            rm -f "$pid_file"
            return 0
        else
            echo -e "${RED}   ❌ Failed to stop${NC}"
            return 1
        fi
    else
        echo -e "${GREEN}   ✅ Service was not running${NC}"
        rm -f "$pid_file"
        return 0
    fi
}

# Show service status
show_status() {
    echo -e "${BLUE}📊 Service Status Overview${NC}"
    echo -e "${WHITE}   Deployment Directory: ${CYAN}$DEPLOY_DIR${NC}"
    echo
    
    printf "%-20s %-8s %-8s %-10s %-15s\n" "SERVICE" "STATUS" "PORT" "PID" "LOG SIZE"
    printf "%-20s %-8s %-8s %-10s %-15s\n" "────────────────────" "──────" "────" "────────" "─────────────"
    
    for service in "${STARTUP_ORDER[@]}"; do
        local status=$(get_service_status "$service")
        local pid=$(get_service_pid "$service")
        local port=${SERVICES[$service]}
        local log_file="$LOG_DIR/${service}.log"
        local log_size="N/A"
        
        if [ -f "$log_file" ]; then
            log_size=$(du -h "$log_file" 2>/dev/null | cut -f1)
        fi
        
        if [ "$status" = "RUNNING" ]; then
            printf "%-20s ${GREEN}%-8s${NC} %-8s %-10s %-15s\n" "$service" "$status" "$port" "$pid" "$log_size"
        else
            printf "%-20s ${RED}%-8s${NC} %-8s %-10s %-15s\n" "$service" "$status" "$port" "$pid" "$log_size"
        fi
    done
    echo
}

# Start all services
start_all() {
    echo -e "${BLUE}🚀 Starting All Services${NC}"
    echo -e "${WHITE}   Starting services in dependency order...${NC}"
    echo
    
    local failed_services=()
    for service in "${STARTUP_ORDER[@]}"; do
        echo -e "${CYAN}▶️  $service${NC}"
        if ! start_service "$service"; then
            failed_services+=("$service")
        fi
        echo
    done
    
    if [ ${#failed_services[@]} -eq 0 ]; then
        echo -e "${GREEN}🎉 All services started successfully!${NC}"
    else
        echo -e "${RED}⚠️  Some services failed to start:${NC}"
        for service in "${failed_services[@]}"; do
            echo -e "${RED}   • $service${NC}"
        done
    fi
}

# Stop all services
stop_all() {
    echo -e "${BLUE}🛑 Stopping All Services${NC}"
    echo -e "${WHITE}   Stopping services in reverse order...${NC}"
    echo
    
    # Reverse the startup order for shutdown
    local reverse_order=()
    for ((i=${#STARTUP_ORDER[@]}-1; i>=0; i--)); do
        reverse_order+=("${STARTUP_ORDER[i]}")
    done
    
    for service in "${reverse_order[@]}"; do
        local status=$(get_service_status "$service")
        if [ "$status" = "RUNNING" ]; then
            echo -e "${CYAN}▶️  $service${NC}"
            stop_service "$service"
            echo
        fi
    done
    
    echo -e "${GREEN}🏁 All services stopped${NC}"
}

# Restart all services
restart_all() {
    echo -e "${BLUE}🔄 Restarting All Services${NC}"
    stop_all
    echo
    sleep 2
    start_all
}

# Show logs for a service
show_logs() {
    local service=$1
    local log_file="$LOG_DIR/${service}.log"
    
    if [ ! -f "$log_file" ]; then
        echo -e "${RED}❌ Log file not found: $log_file${NC}"
        return 1
    fi
    
    echo -e "${BLUE}📋 Showing logs for $service${NC}"
    echo -e "${WHITE}   Log file: $log_file${NC}"
    echo -e "${WHITE}   Press Ctrl+C to exit${NC}"
    echo
    
    tail -f "$log_file"
}

# Health check
health_check() {
    echo -e "${BLUE}🏥 Health Check${NC}"
    echo
    
    for service in "${STARTUP_ORDER[@]}"; do
        local status=$(get_service_status "$service")
        local port=${SERVICES[$service]}
        
        if [ "$status" = "RUNNING" ]; then
            echo -e "${WHITE}Checking $service on port $port...${NC}"
            
            # Try to connect to the service
            if timeout 5 bash -c "</dev/tcp/localhost/$port" 2>/dev/null; then
                echo -e "${GREEN}   ✅ Port $port is accessible${NC}"
            else
                echo -e "${RED}   ❌ Port $port is not accessible${NC}"
            fi
        else
            echo -e "${YELLOW}   ⚠️  $service is not running${NC}"
        fi
    done
}

# Show usage
show_usage() {
    echo -e "${WHITE}Usage: $0 [command] [service] OR $0 [deployment_directory] [command] [service]${NC}"
    echo
    echo -e "${CYAN}Commands:${NC}"
    echo -e "${WHITE}  start [service]     ${NC}Start all services or specific service"
    echo -e "${WHITE}  stop [service]      ${NC}Stop all services or specific service"
    echo -e "${WHITE}  restart [service]   ${NC}Restart all services or specific service"
    echo -e "${WHITE}  status              ${NC}Show service status"
    echo -e "${WHITE}  logs <service>      ${NC}Show logs for a service"
    echo -e "${WHITE}  health              ${NC}Perform health check"
    echo -e "${WHITE}  help                ${NC}Show this help message"
    echo
    echo -e "${CYAN}Examples:${NC}"
    echo -e "${WHITE}  $0 start                         ${NC}Start all services"
    echo -e "${WHITE}  $0 start eureka-service          ${NC}Start only eureka-service"
    echo -e "${WHITE}  $0 stop                          ${NC}Stop all services"
    echo -e "${WHITE}  $0 logs user-service             ${NC}Show logs for user-service"
    echo -e "${WHITE}  $0 status                        ${NC}Show status of all services"
    echo -e "${WHITE}  $0 /custom/path start            ${NC}Start all with custom directory"
    echo -e "${WHITE}  $0 /custom/path start user-service ${NC}Start specific service with custom directory"
}

# Main script logic
main() {
    # Set default deployment directory
    DEPLOY_DIR="$HOME/Desktop/phinity-deployment"
    
    # Parse arguments
    local command="$1"
    local service="$2"
    
    # If first argument is a directory path, shift arguments
    if [[ "$1" == /* ]] && [[ -d "$1" ]]; then
        DEPLOY_DIR="$1"
        command="$2"
        service="$3"
    fi
    
    # Default to status if no command provided
    command=${command:-status}
    
    # Check if deployment directory exists
    if [ ! -d "$DEPLOY_DIR" ]; then
        echo -e "${RED}❌ Deployment directory not found: $DEPLOY_DIR${NC}"
        echo -e "${WHITE}💡 Run the package.sh script first to create deployment artifacts${NC}"
        exit 1
    fi
    
    # Set directory paths after DEPLOY_DIR is confirmed
    PID_DIR="$DEPLOY_DIR/pids"
    LOG_DIR="$DEPLOY_DIR/logs"
    CONFIG_FILE="$DEPLOY_DIR/services.conf"
    
    init_directories
    load_config
    
    case $command in
        "start")
            if [ -n "$service" ]; then
                if [[ " ${!SERVICES[@]} " =~ " $service " ]]; then
                    echo -e "${CYAN}▶️  $service${NC}"
                    start_service "$service"
                else
                    echo -e "${RED}❌ Unknown service: $service${NC}"
                    exit 1
                fi
            else
                start_all
            fi
            ;;
        "stop")
            if [ -n "$service" ]; then
                if [[ " ${!SERVICES[@]} " =~ " $service " ]]; then
                    echo -e "${CYAN}▶️  $service${NC}"
                    stop_service "$service"
                else
                    echo -e "${RED}❌ Unknown service: $service${NC}"
                    exit 1
                fi
            else
                stop_all
            fi
            ;;
        "restart")
            if [ -n "$service" ]; then
                if [[ " ${!SERVICES[@]} " =~ " $service " ]]; then
                    echo -e "${CYAN}▶️  $service${NC}"
                    stop_service "$service"
                    echo
                    start_service "$service"
                else
                    echo -e "${RED}❌ Unknown service: $service${NC}"
                    exit 1
                fi
            else
                restart_all
            fi
            ;;
        "status")
            show_status
            ;;
        "logs")
            if [ -n "$service" ]; then
                if [[ " ${!SERVICES[@]} " =~ " $service " ]]; then
                    show_logs "$service"
                else
                    echo -e "${RED}❌ Unknown service: $service${NC}"
                    exit 1
                fi
            else
                echo -e "${RED}❌ Please specify a service name${NC}"
                echo -e "${WHITE}Available services: ${!SERVICES[*]}${NC}"
                exit 1
            fi
            ;;
        "health")
            health_check
            ;;
        "help"|"-h"|"--help")
            show_usage
            ;;
        *)
            echo -e "${RED}❌ Unknown command: $command${NC}"
            echo
            show_usage
            exit 1
            ;;
    esac
}

# Run main function
main "$@"

echo
echo -e "${GREEN}✨ Phinity Exchange Deployment Manager ✨${NC}"