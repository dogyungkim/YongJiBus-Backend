#!/bin/bash

# WithYou 로컬 Docker 배포 파이프라인
# 작성자: WithYou Team
# 설명: 로컬에서 테스트 실행 → Docker 빌드 → 버전 히스토리 업데이트 → SSH 서버 배포

set -e  # 에러 발생 시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# 로그 함수
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_step() {
    echo -e "${PURPLE}[STEP]${NC} $1"
}

# Docker 이미지 정리 함수
cleanup_docker_images() {
    local current_tag="$1"
    local repository="prunsoli/yongji-backend"
    
    # 현재 사용 중인 이미지를 제외한 이전 버전들 찾기
    local old_images=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep "$repository" | grep -v "$current_tag" || true)
    
    if [ ! -z "$old_images" ]; then
        echo "🗑️  삭제할 이전 이미지들:"
        echo "$old_images"
        
        # 이미지 삭제 시도
        local deleted_count=0
        while IFS= read -r image; do
            if docker rmi -f "$image" 2>/dev/null; then
                deleted_count=$((deleted_count + 1))
                log_info "삭제됨: $image"
            else
                log_warning "삭제 실패 (사용 중일 수 있음): $image"
            fi
        done <<< "$old_images"
        
        if [ $deleted_count -gt 0 ]; then
            log_success "✅ $deleted_count 개의 이전 버전 이미지 삭제 완료"
        fi
    else
        log_info "📝 정리할 이전 이미지가 없습니다."
    fi
}

# 설정 변수
DOCKER_REGISTRY="prunsoli"
PROJECT_NAME="yongji-backend"
DOCKER_HISTORY_FILE="Docker-History.md"
CURRENT_BRANCH=$(git branch --show-current)
CURRENT_DATE=$(date '+%Y-%m-%d')
CURRENT_TIME=$(date '+%H:%M:%S')

# 환경 설정 파일 로드 (있는 경우)
if [ -f "deploy.env" ]; then
    log_info "환경 설정 파일 로드: deploy.env"
    source deploy.env
fi

# 기본 태그 설정 (인자로 받거나 기본값 사용)
if [ -z "$1" ]; then
    # 현재 시간 기반으로 태그 생성 (예: 0.0.5-20250115-192300)
    TIMESTAMP=$(date '+%Y%m%d-%H%M%S')
    VERSION_TAG="0.0.5-${TIMESTAMP}"
else
    VERSION_TAG="$1"
fi

# SSH 서버 정보 (환경변수로 설정 권장)
SSH_HOST="${SSH_HOST:-your-server.com}"
SSH_USER="${SSH_USER:-ubuntu}"
SSH_KEY_PATH="${SSH_KEY_PATH:-~/.ssh/id_rsa}"
REMOTE_COMPOSE_PATH="${REMOTE_COMPOSE_PATH:-/opt/withyou}"

# 배포 설명 (선택적)
DEPLOY_DESCRIPTION="${2:-Manual local deployment}"

echo ""
echo "🚀 ============================================="
echo "🚀   WithYou 로컬 Docker 배포 파이프라인"
echo "🚀 ============================================="
echo ""
log_info "브랜치: ${CURRENT_BRANCH}"
log_info "버전 태그: ${VERSION_TAG}"
log_info "Docker 이미지: ${DOCKER_REGISTRY}/${PROJECT_NAME}:${VERSION_TAG}"
log_info "배포 설명: ${DEPLOY_DESCRIPTION}"
log_info "SSH 서버: ${SSH_HOST}"
echo ""

# 사전 체크
log_step "사전 체크 진행 중..."

# Java 및 Gradle 체크
if ! command -v java &> /dev/null; then
    log_error "Java가 설치되어 있지 않습니다."
    exit 1
fi

if ! ./gradlew --version &> /dev/null; then
    log_error "Gradle 실행에 실패했습니다."
    exit 1
fi

# Docker 체크
if ! command -v docker &> /dev/null; then
    log_error "Docker가 설치되어 있지 않습니다."
    exit 1
fi

if ! docker info &> /dev/null; then
    log_error "Docker 데몬이 실행되고 있지 않습니다."
    exit 1
fi

log_success "사전 체크 완료"

# 1. 테스트 실행
log_step "1단계: 테스트 실행 중..."
echo "📋 Gradle 테스트를 실행합니다..."

if ./gradlew clean test --no-daemon --info; then
    log_success "✅ 모든 테스트 통과!"
else
    log_error "❌ 테스트 실패! 배포를 중단합니다."
    echo ""
    echo "테스트 실패 시 확인사항:"
    echo "1. ./gradlew test --info 로 상세 로그 확인"
    echo "2. build/reports/tests/test/index.html 에서 테스트 리포트 확인"
    exit 1
fi

# 2. Docker 이미지 빌드 및 푸시
log_step "2단계: Docker 이미지 빌드 및 푸시 중..."
DOCKER_IMAGE="${DOCKER_REGISTRY}/${PROJECT_NAME}:${VERSION_TAG}"

echo "🐳 Docker 이미지 빌드를 시작합니다..."
echo "   이미지: ${DOCKER_IMAGE}"

# Docker buildx를 사용하여 multi-platform 빌드
DOCKER_COMMAND="docker buildx build --platform linux/amd64 -t ${DOCKER_IMAGE} --push ."

echo "실행 명령어: ${DOCKER_COMMAND}"

if eval "${DOCKER_COMMAND}"; then
    log_success "✅ Docker 이미지 빌드 및 푸시 완료: ${DOCKER_IMAGE}"
else
    log_error "❌ Docker 이미지 빌드 실패!"
    echo ""
    echo "Docker 빌드 실패 시 확인사항:"
    echo "1. Docker Hub 로그인 상태 확인: docker login"
    echo "2. 네트워크 연결 상태 확인"
    echo "3. Dockerfile 문법 오류 확인"
    exit 1
fi

# 3. Docker-History.md 업데이트
log_step "3단계: Docker-History.md 파일 업데이트 중..."

# 현재 커밋 메시지 가져오기
COMMIT_MESSAGE=$(git log -1 --pretty=format:"%s" 2>/dev/null || echo "${DEPLOY_DESCRIPTION}")

# 백업 생성
if [ -f "${DOCKER_HISTORY_FILE}" ]; then
    cp "${DOCKER_HISTORY_FILE}" "${DOCKER_HISTORY_FILE}.backup"
    log_info "기존 히스토리 파일 백업 생성: ${DOCKER_HISTORY_FILE}.backup"
fi

# 새로운 엔트리를 기존 파일의 7번째 줄 다음에 삽입
TEMP_FILE=$(mktemp)

# 헤더 부분 (1-6줄)
head -n 6 "${DOCKER_HISTORY_FILE}" > "${TEMP_FILE}"

# 새로운 엔트리 추가
echo "| ${CURRENT_DATE} | ${CURRENT_TIME} | \`${DOCKER_REGISTRY}/${PROJECT_NAME}:${VERSION_TAG}\` | \`${DOCKER_COMMAND}\` | linux/amd64 | ${COMMIT_MESSAGE} |" >> "${TEMP_FILE}"

# 기존 데이터 부분 (8줄부터 끝까지)
tail -n +7 "${DOCKER_HISTORY_FILE}" >> "${TEMP_FILE}"

# 원본 파일 교체
mv "${TEMP_FILE}" "${DOCKER_HISTORY_FILE}"

log_success "✅ Docker-History.md 업데이트 완료"

# Docker 빌드가 성공했으므로 백업 파일 삭제
if [ -f "${DOCKER_HISTORY_FILE}.backup" ]; then
    rm "${DOCKER_HISTORY_FILE}.backup"
    log_info "백업 파일 삭제: ${DOCKER_HISTORY_FILE}.backup"
fi

# 4. SSH를 통한 서버 배포
log_step "4단계: SSH를 통한 서버 배포 시작..."

if [ "${SSH_HOST}" == "your-server.com" ]; then
    log_warning "⚠️  SSH 호스트가 설정되지 않았습니다."
    log_warning "deploy.env 파일을 생성하고 SSH_HOST, SSH_USER, SSH_KEY_PATH를 설정하세요."
    log_warning ""
else
    # SSH 연결 테스트
    log_info "SSH 연결 테스트 중: ${SSH_USER}@${SSH_HOST}"
    
    if ! ssh -o ConnectTimeout=10 -o BatchMode=yes -i "${SSH_KEY_PATH}" "${SSH_USER}@${SSH_HOST}" "echo 'SSH 연결 성공'" >/dev/null 2>&1; then
        log_error "❌ SSH 연결 실패"
        log_warning "다음 사항을 확인하세요:"
        log_warning "1. SSH 키 경로: ${SSH_KEY_PATH}"
        log_warning "2. 서버 주소: ${SSH_HOST}"
        log_warning "3. 사용자명: ${SSH_USER}"
        log_warning "4. 서버의 SSH 포트가 열려있는지 확인"
        echo ""
        exit 1
    else
        log_success "✅ SSH 연결 성공"
        log_info "서버에 새 이미지 배포 중..."
        
        # 서버에서 실행할 배포 스크립트
        ssh -i "${SSH_KEY_PATH}" "${SSH_USER}@${SSH_HOST}" << EOF
            set -e
            echo "🚀 서버 배포 시작..."
            
            # 작업 디렉토리로 이동
            cd ${REMOTE_COMPOSE_PATH} || { echo "❌ 디렉토리 이동 실패: ${REMOTE_COMPOSE_PATH}"; exit 1; }
            
            # 새 이미지 pull
            echo "📦 새 Docker 이미지 다운로드 중: ${DOCKER_IMAGE}"
            docker pull ${DOCKER_IMAGE}
            
            # docker-compose.yml에서 이미지 태그 업데이트
            if [ -f docker-compose.yml ]; then
                # 백업 생성
                cp docker-compose.yml docker-compose.yml.backup.\$(date +%Y%m%d_%H%M%S)
                
                # 이미지 태그 업데이트
                sed -i "s|image: prunsoli/withyou-test:.*|image: ${DOCKER_IMAGE}|g" docker-compose.yml
                
                echo "✅ docker-compose.yml 이미지 태그 업데이트 완료"
                echo "📋 업데이트된 이미지: ${DOCKER_IMAGE}"
                
                # 서비스 재시작
                echo "🔄 서비스 재시작 중..."
                docker-compose down
                docker-compose up -d
                
                echo "📊 서비스 상태 확인..."
                docker-compose ps
                
                # 헬스체크 (선택적)
                echo "🏥 헬스체크 대기 중 (10초)..."
                sleep 10
                
                if docker-compose ps | grep -q "Up"; then
                    echo "✅ 서비스가 정상적으로 실행 중입니다."
                    
                    # 배포 성공 시 불필요한 Docker 이미지 정리
                    echo "🧹 불필요한 Docker 이미지 정리 중..."
                    
                    # 서버에서 이전 버전 이미지들 정리
                    OLD_IMAGES=\$(docker images --format "{{.Repository}}:{{.Tag}}" | grep "prunsoli/withyou-test" | grep -v "${VERSION_TAG}" || true)
                    
                    if [ ! -z "\$OLD_IMAGES" ]; then
                        echo "🗑️ 삭제할 서버의 이전 이미지들:"
                        echo "\$OLD_IMAGES"
                        
                        deleted_count=0
                        for image in \$OLD_IMAGES; do
                            if docker rmi -f "\$image" 2>/dev/null; then
                                deleted_count=\$((deleted_count + 1))
                                echo "삭제됨: \$image"
                            else
                                echo "삭제 실패 (사용 중일 수 있음): \$image"
                            fi
                        done
                        
                        if [ \$deleted_count -gt 0 ]; then
                            echo "✅ \$deleted_count 개의 서버 이전 버전 이미지 삭제 완료"
                        fi
                    else
                        echo "📝 서버에 정리할 이전 이미지가 없습니다."
                    fi
                    
                    # 사용하지 않는 이미지, 컨테이너, 네트워크, 볼륨 정리
                    docker system prune -f >/dev/null 2>&1 || true
                    echo "✅ Docker 시스템 정리 완료"
                    
                else
                    echo "⚠️  서비스 상태를 확인하세요."
                    docker-compose logs --tail=20
                fi
                
            else
                echo "❌ docker-compose.yml 파일이 없습니다."
                echo "수동으로 컨테이너를 재시작하세요:"
                echo "docker stop withyou-app || true"
                echo "docker rm withyou-app || true"
                echo "docker run -d --name withyou-app -p 8080:8080 ${DOCKER_IMAGE}"
            fi
            
            echo "🎉 서버 배포 완료!"
EOF
        
        if [ $? -eq 0 ]; then
            log_success "✅ 서버 배포 완료!"
        else
            log_error "❌ 서버 배포 중 오류 발생"
            exit 1
        fi
    fi
fi

# 5. 로컬 환경 정리 작업
log_step "5단계: 로컬 환경 정리 작업..."

# 배포가 성공했다면 자동으로 정리, 실패했다면 선택적으로 정리
if [ "${SSH_HOST}" != "your-server.com" ] && ssh -o ConnectTimeout=5 -o BatchMode=yes -i "${SSH_KEY_PATH}" "${SSH_USER}@${SSH_HOST}" "docker-compose ps | grep -q 'Up'" 2>/dev/null; then
    # 서버 배포가 성공한 경우 자동 정리
    log_info "서버 배포 성공 확인됨. 로컬 Docker 이미지 자동 정리 중..."
    
    # 현재 배포한 이미지를 제외한 이전 버전들 정리
    cleanup_docker_images "${VERSION_TAG}"
    
    # 사용하지 않는 Docker 리소스 정리
    docker system prune -f >/dev/null 2>&1 || true
    log_success "✅ Docker 시스템 정리 완료"
    
else
    # 서버 배포 실패 또는 SSH 설정이 안된 경우 선택적 정리
    echo "🧹 로컬 Docker 이미지를 정리하시겠습니까? (y/N): "
    read -r CLEANUP_CONFIRM
    
    if [[ $CLEANUP_CONFIRM =~ ^[Yy]$ ]]; then
        log_info "사용하지 않는 Docker 이미지 정리 중..."
        
        # 현재 배포한 이미지를 제외한 이전 버전들 정리
        cleanup_docker_images "${VERSION_TAG}"
        
        docker system prune -f >/dev/null 2>&1 || true
        log_success "✅ Docker 시스템 정리 완료"
    else
        log_info "Docker 이미지 정리를 건너뜁니다."
    fi
fi

# 최종 결과 출력
echo ""
echo "🎉 ============================================="
echo "🎉   배포가 성공적으로 완료되었습니다!"
echo "🎉 ============================================="
echo ""
echo "📦 배포된 이미지: ${DOCKER_IMAGE}"
echo "🌐 서버: ${SSH_HOST}"
echo "📝 히스토리: ${DOCKER_HISTORY_FILE} 업데이트됨"
echo "⏰ 배포 시간: $(date)"
echo ""

if [ "${SSH_HOST}" != "your-server.com" ]; then
    echo "🔍 서비스 상태 확인:"
    echo "   curl -f http://${SSH_HOST}:8080/actuator/health"
    echo "   또는"
    echo "   curl -f http://${SSH_HOST}:8080/"
fi

echo ""
echo "📚 다음 배포 시 참고사항:"
echo "   ./deploy-local.sh [버전태그] [배포설명]"
echo "   예: ./deploy-local.sh 0.0.6 \"로그인 기능 추가\""
echo ""
echo "🧹 Docker 이미지 관리:"
echo "   - 성공적인 배포 시 이전 버전 이미지 자동 정리"
echo "   - 수동 정리: docker images | grep prunsoli/withyou-test"
echo "   - 전체 정리: docker system prune -a"
echo ""