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

# Service/Module definitions
declare -A SERVICES=(
    ["eureka-service"]="eureka-service"
    ["gateway-service"]="gateway-service"
    ["user-service"]="user-service"
    ["market-service"]="market-service"
    ["admin-service"]="admin-service"
    ["matching-service"]="matching-service"
    ["order-service"]="order-service"
    ["tradingview-service"]="tradingview-service"
    ["wallet-service"]="wallet-service"
    ["websocket-service"]="websocket-service"
    ["matching-engine"]="matching-engine"
)

declare -A MODULES=(
    ["dto-module"]="common-modules/dto-module"
    ["config-module"]="common-modules/config-module"
    ["utils-module"]="common-modules/utils-module"
    ["jwt-module"]="common-modules/jwt-module"
    ["email-module"]="common-modules/email-module"
    ["sms-module"]="common-modules/sms-module"
    ["file-module"]="common-modules/file-module"
    ["mongo-module"]="database-module/mongo-module"
    ["redis-module"]="database-module/redis-module"
    ["postgre-module"]="database-module/postgre-module"
    ["kafka-module"]="kafka-module"
)

# Configuration
DEPLOY_DIR=""
TARGET_SERVICE=""
TARGET_MODULE=""

# Clear screen and show header
clear
echo -e "${PURPLE}"
echo "╔══════════════════════════════════════════════════════════════════════════════╗"
echo "║                        🚀 PHINITY EXCHANGE BUILDER 🚀                        ║"
echo "║                           Trading Platform Packager                          ║"
echo "║                              Version 1.0.0                                   ║"
echo "╚══════════════════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo

# Build specific service
build_service() {
    local service=$1
    local service_path=${SERVICES[$service]}
    
    if [ ! -d "$service_path" ]; then
        echo -e "${RED}❌ Service directory not found: $service_path${NC}"
        return 1
    fi
    
    echo -e "${WHITE}   🔨 Building $service...${NC}"
    cd "$service_path"
    mvn clean package -q -DskipTests
    local result=$?
    cd - > /dev/null
    
    if [ $result -eq 0 ]; then
        echo -e "${GREEN}   ✅ $service built successfully${NC}"
        return 0
    else
        echo -e "${RED}   ❌ $service build failed${NC}"
        return 1
    fi
}

# Build specific module
build_module() {
    local module=$1
    local module_path=${MODULES[$module]}
    
    if [ ! -d "$module_path" ]; then
        echo -e "${RED}❌ Module directory not found: $module_path${NC}"
        return 1
    fi
    
    echo -e "${WHITE}   🔨 Building $module...${NC}"
    cd "$module_path"
    mvn clean install -q -DskipTests
    local result=$?
    cd - > /dev/null
    
    if [ $result -eq 0 ]; then
        echo -e "${GREEN}   ✅ $module built successfully${NC}"
        return 0
    else
        echo -e "${RED}   ❌ $module build failed${NC}"
        return 1
    fi
}

# Build all services and modules
build_all() {
    echo -e "${BLUE}📦 Building All Modules and Services${NC}"
    echo -e "${WHITE}   Building in dependency order...${NC}"
    echo
    
    # First build all modules in dependency order
    echo -e "${CYAN}🧩 Building Modules${NC}"
    local failed_modules=()
    
    # Build modules in correct dependency order
    local module_order=(
        "dto-module"
        "config-module"
        "utils-module"
        "jwt-module"
        "email-module"
        "sms-module"
        "file-module"
        "mongo-module"
        "redis-module"
        "postgre-module"
        "kafka-module"
    )
    
    for module in "${module_order[@]}"; do
        if ! build_module "$module"; then
            failed_modules+=("$module")
        fi
    done
    
    if [ ${#failed_modules[@]} -gt 0 ]; then
        echo -e "${RED}❌ Some modules failed to build:${NC}"
        for module in "${failed_modules[@]}"; do
            echo -e "${RED}   • $module${NC}"
        done
        return 1
    fi
    
    echo
    echo -e "${CYAN}🚀 Building Services${NC}"
    local failed_services=()
    for service in "${!SERVICES[@]}"; do
        if ! build_service "$service"; then
            failed_services+=("$service")
        fi
    done
    
    if [ ${#failed_services[@]} -gt 0 ]; then
        echo -e "${RED}❌ Some services failed to build:${NC}"
        for service in "${failed_services[@]}"; do
            echo -e "${RED}   • $service${NC}"
        done
        return 1
    fi
    
    return 0
}

# Copy artifacts to deployment directory
copy_artifacts() {
    echo -e "${BLUE}🚚 Copying Artifacts${NC}"
    echo -e "${WHITE}   Collecting and copying JAR files...${NC}"
    
    mkdir -p "$DEPLOY_DIR"
    
    local jar_count=0
    if [ -n "$TARGET_SERVICE" ]; then
        # Copy specific service JAR
        local service_path=${SERVICES[$TARGET_SERVICE]}
        local jar_file=$(find "$service_path/target" -name "*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" ! -name "*-tests.jar" 2>/dev/null | head -1)
        if [ -f "$jar_file" ]; then
            cp "$jar_file" "$DEPLOY_DIR/"
            jar_count=1
            echo -e "${GREEN}   ✅ Copied $TARGET_SERVICE artifact${NC}"
        else
            echo -e "${RED}   ❌ No JAR found for $TARGET_SERVICE${NC}"
            return 1
        fi
    elif [ -n "$TARGET_MODULE" ]; then
        # Copy specific module JAR
        local module_path=${MODULES[$TARGET_MODULE]}
        local jar_file=$(find "$module_path/target" -name "*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" ! -name "*-tests.jar" 2>/dev/null | head -1)
        if [ -f "$jar_file" ]; then
            cp "$jar_file" "$DEPLOY_DIR/"
            jar_count=1
            echo -e "${GREEN}   ✅ Copied $TARGET_MODULE artifact${NC}"
        else
            echo -e "${GREEN}   ℹ️  No JAR artifact for $TARGET_MODULE (library module)${NC}"
        fi
    else
        # Copy all JARs
        for jar in $(find . -name "*.jar" -path "*/target/*" ! -name "*-sources.jar" ! -name "*-javadoc.jar" ! -name "*-tests.jar"); do
            cp "$jar" "$DEPLOY_DIR/"
            jar_count=$((jar_count + 1))
        done
        echo -e "${GREEN}   ✅ Copied $jar_count service artifacts${NC}"
    fi
    
    return 0
}

# Show usage
show_usage() {
    echo -e "${WHITE}Usage: $0 [deployment_directory] [service|module] OR $0 [service|module]${NC}"
    echo
    echo -e "${CYAN}Examples:${NC}"
    echo -e "${WHITE}  $0                              ${NC}Build all services and modules"
    echo -e "${WHITE}  $0 user-service                ${NC}Build only user-service"
    echo -e "${WHITE}  $0 dto-module                  ${NC}Build only dto-module"
    echo -e "${WHITE}  $0 /custom/path                ${NC}Build all with custom directory"
    echo -e "${WHITE}  $0 /custom/path user-service   ${NC}Build user-service with custom directory"
    echo
    echo -e "${CYAN}Available Services:${NC}"
    for service in "${!SERVICES[@]}"; do
        echo -e "${WHITE}  • $service${NC}"
    done
    echo
    echo -e "${CYAN}Available Modules:${NC}"
    for module in "${!MODULES[@]}"; do
        echo -e "${WHITE}  • $module${NC}"
    done
}

# Main function
main() {
    # Set default deployment directory
    DEPLOY_DIR="$HOME/Desktop/phinity-deployment"
    
    # Parse arguments
    local target="$1"
    local second_arg="$2"
    
    # If first argument is a directory path, shift arguments
    if [[ "$1" == /* ]] && [[ -d "$(dirname "$1")" ]]; then
        DEPLOY_DIR="$1"
        target="$2"
    fi
    
    # Check if target is a service or module
    if [ -n "$target" ]; then
        if [[ " ${!SERVICES[@]} " =~ " $target " ]]; then
            TARGET_SERVICE="$target"
        elif [[ " ${!MODULES[@]} " =~ " $target " ]]; then
            TARGET_MODULE="$target"
        elif [ "$target" != "help" ] && [ "$target" != "-h" ] && [ "$target" != "--help" ]; then
            echo -e "${RED}❌ Unknown service or module: $target${NC}"
            echo
            show_usage
            exit 1
        fi
    fi
    
    # Show help
    if [ "$target" = "help" ] || [ "$target" = "-h" ] || [ "$target" = "--help" ]; then
        show_usage
        exit 0
    fi
    
    echo -e "${WHITE}📁 Deployment Directory: ${CYAN}$DEPLOY_DIR${NC}"
    
    if [ -n "$TARGET_SERVICE" ]; then
        echo -e "${WHITE}🎯 Target Service: ${CYAN}$TARGET_SERVICE${NC}"
    elif [ -n "$TARGET_MODULE" ]; then
        echo -e "${WHITE}🎯 Target Module: ${CYAN}$TARGET_MODULE${NC}"
    else
        echo -e "${WHITE}🎯 Target: ${CYAN}All Services and Modules${NC}"
    fi
    
    echo
    echo -e "${YELLOW}⚠️  This will build and package the selected components.${NC}"
    echo -e "${WHITE}   Continue? (y/N): ${NC}\c"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo -e "${RED}❌ Build cancelled.${NC}"
        exit 1
    fi
    
    echo
    echo -e "${GREEN}🔧 Starting build process...${NC}"
    echo
    
    # Build based on target
    if [ -n "$TARGET_SERVICE" ]; then
        echo -e "${BLUE}📦 Building Service: $TARGET_SERVICE${NC}"
        echo -e "${WHITE}   Building required modules first...${NC}"
        
        # Build all modules first for any service
        local module_order=(
            "dto-module"
            "config-module"
            "utils-module"
            "jwt-module"
            "email-module"
            "sms-module"
            "file-module"
            "mongo-module"
            "redis-module"
            "postgre-module"
            "kafka-module"
        )
        
        for module in "${module_order[@]}"; do
            if ! build_module "$module"; then
                echo -e "${RED}❌ Module build failed: $module${NC}"
                exit 1
            fi
        done
        
        echo -e "${WHITE}   Building service...${NC}"
        if ! build_service "$TARGET_SERVICE"; then
            echo -e "${RED}❌ Build failed${NC}"
            exit 1
        fi
    elif [ -n "$TARGET_MODULE" ]; then
        echo -e "${BLUE}📦 Building Module: $TARGET_MODULE${NC}"
        if ! build_module "$TARGET_MODULE"; then
            echo -e "${RED}❌ Build failed${NC}"
            exit 1
        fi
    else
        if ! build_all; then
            echo -e "${RED}❌ Build failed${NC}"
            exit 1
        fi
    fi
    
    echo
    echo -e "${BLUE}📁 Creating Deployment Package${NC}"
    echo -e "${WHITE}   Setting up deployment directory...${NC}"
    
    if ! copy_artifacts; then
        echo -e "${RED}❌ Failed to copy artifacts${NC}"
        exit 1
    fi
    
    echo
    echo -e "${GREEN}"
    echo "╔══════════════════════════════════════════════════════════════════════════════╗"
    echo "║                        🎉 BUILD SUCCESSFUL! 🎉                              ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo
    
    echo -e "${WHITE}📊 Build Summary:${NC}"
    if [ -n "$TARGET_SERVICE" ]; then
        echo -e "${CYAN}   • Service Built: ${WHITE}$TARGET_SERVICE${NC}"
    elif [ -n "$TARGET_MODULE" ]; then
        echo -e "${CYAN}   • Module Built: ${WHITE}$TARGET_MODULE${NC}"
    else
        echo -e "${CYAN}   • Services Built: ${WHITE}${#SERVICES[@]}${NC}"
        echo -e "${CYAN}   • Modules Built: ${WHITE}${#MODULES[@]}${NC}"
    fi
    echo -e "${CYAN}   • Deployment Path: ${WHITE}$DEPLOY_DIR${NC}"
    echo -e "${CYAN}   • Total Size: ${WHITE}$(du -sh "$DEPLOY_DIR" 2>/dev/null | cut -f1)${NC}"
    echo
    
    if ls "$DEPLOY_DIR"/*.jar >/dev/null 2>&1; then
        echo -e "${WHITE}🚀 Ready to deploy:${NC}"
        ls -1 "$DEPLOY_DIR"/*.jar 2>/dev/null | sed 's|.*/||' | sed 's/^/   • /'
        echo
    fi
    
    echo -e "${YELLOW}💡 Next Steps:${NC}"
    echo -e "${WHITE}   1. Use deploy.sh to start services${NC}"
    echo -e "${WHITE}   2. Configure environment variables if needed${NC}"
    echo -e "${WHITE}   3. Monitor logs for successful startup${NC}"
    echo
    echo -e "${GREEN}✨ Thank you for choosing Phinity Exchange! ✨${NC}"
}

# Run main function
main "$@"