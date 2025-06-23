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

# Clear screen and show header
clear
echo -e "${PURPLE}"
echo "╔══════════════════════════════════════════════════════════════════════════════╗"
echo "║                        🚀 PHINITY EXCHANGE BUILDER 🚀                        ║"
echo "║                           Trading Platform Installer                         ║"
echo "║                              Version 1.0.0                                   ║"
echo "╚══════════════════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo

DEPLOY_DIR="$1"
if [ -z "$DEPLOY_DIR" ]; then
    DEPLOY_DIR="$HOME/Desktop/phinity-deployment"
fi

echo -e "${WHITE}📁 Deployment Directory: ${CYAN}$DEPLOY_DIR${NC}"
echo
echo -e "${YELLOW}⚠️  This will build and package all Phinity Exchange services.${NC}"
echo -e "${WHITE}   Continue? (y/N): ${NC}\c"
read -r response
if [[ ! "$response" =~ ^[Yy]$ ]]; then
    echo -e "${RED}❌ Installation cancelled.${NC}"
    exit 1
fi

echo
echo -e "${GREEN}🔧 Starting build process...${NC}"
echo

echo -e "${BLUE}📦 Phase 1/3: Building Services${NC}"
echo -e "${WHITE}   Compiling source code and resolving dependencies...${NC}"
mvn clean install -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}   ✅ Build completed successfully${NC}"
else
    echo -e "${RED}   ❌ Build failed${NC}"
    exit 1
fi
echo

echo -e "${BLUE}📁 Phase 2/3: Creating Deployment Package${NC}"
echo -e "${WHITE}   Setting up deployment directory...${NC}"
mkdir -p "$DEPLOY_DIR"
echo -e "${GREEN}   ✅ Directory created: $DEPLOY_DIR${NC}"
echo

echo -e "${BLUE}🚚 Phase 3/3: Copying Service Artifacts${NC}"
echo -e "${WHITE}   Collecting and copying JAR files...${NC}"

JAR_COUNT=0
for jar in $(find . -name "*.jar" -path "*/target/*" ! -name "*-sources.jar" ! -name "*-javadoc.jar" ! -name "*-tests.jar"); do
    cp "$jar" "$DEPLOY_DIR/"
    JAR_COUNT=$((JAR_COUNT + 1))
done

echo -e "${GREEN}   ✅ Copied $JAR_COUNT service artifacts${NC}"
echo

echo -e "${GREEN}"
echo "╔══════════════════════════════════════════════════════════════════════════════╗"
echo "║                        🎉 INSTALLATION SUCCESSFUL! 🎉                       ║"
echo "╚══════════════════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo
echo -e "${WHITE}📊 Installation Summary:${NC}"
echo -e "${CYAN}   • Services Built: ${WHITE}$JAR_COUNT${NC}"
echo -e "${CYAN}   • Deployment Path: ${WHITE}$DEPLOY_DIR${NC}"
echo -e "${CYAN}   • Total Size: ${WHITE}$(du -sh "$DEPLOY_DIR" 2>/dev/null | cut -f1)${NC}"
echo
echo -e "${WHITE}🚀 Ready to deploy services:${NC}"
ls -1 "$DEPLOY_DIR"/*.jar 2>/dev/null | sed 's|.*/||' | sed 's/^/   • /'
echo
echo -e "${YELLOW}💡 Next Steps:${NC}"
echo -e "${WHITE}   1. Configure your environment variables${NC}"
echo -e "${WHITE}   2. Start services using: java -jar <service-name>.jar${NC}"
echo -e "${WHITE}   3. Monitor logs for successful startup${NC}"
echo
echo -e "${GREEN}✨ Thank you for choosing Phinity Exchange! ✨${NC}"