# Docker Image Version History

## Version History

| Version | Date | Description | Image Tag | Changes |
|---------|------|-------------|-----------|---------|
| 0.0.18 | 2025-06-17 | HTTPS 적용 과정 | `yongji-backend:0.0.18` | AWS 타겟그룹 Health Check 기능 추가 |
| 0.0.19 | 2025-06-17 | HTTPS 적용 과정 | `yongji-backend:0.0.19` | Spring Security Health 엔드포인트 추가 |
| 0.0.20 | 2025-06-17 | HTTPS 적용 과정 | `yongji-backend:0.0.20` | HealthController 오류 해결 |


## Version Naming Convention

- 메이저 버전: 큰 기능 변경이나 호환되지 않는 API 변경
- 마이너 버전: 이전 버전과 호환되는 기능 추가
- 패치 버전: 버그 수정

예시: `1.0.0` (메이저.마이너.패치)

## 주의사항

1. 새로운 버전을 배포할 때마다 이 문서를 업데이트해주세요.
2. 주요 변경사항은 반드시 Description과 Changes 컬럼에 기록해주세요.
3. 이미지 태그는 반드시 버전과 일치시켜주세요. 