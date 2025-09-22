# Docker Image Version History

## Version History

| Date       | Time     | Image                               | Docker Command                 | Platform   | Message |
|------------|----------|-------------------------------------|----------------------------------|------------|---------|
| 2025-09-23 | 00:38:34 | `prunsoli/yongji-backend:0.0.22` | `docker buildx build --platform linux/amd64 -t prunsoli/yongji-backend:0.0.22 --push .` | linux/amd64 | feat: Bounce Mail 처리 |
| 2025-09-22 | 00:45:42 | `prunsoli/yongji-backend:0.0.21`    | `이미지 존재 (빌드 건너뜀)`       | linux/amd64 | feat: 배포 스크립트 작성 |
| 2025-06-17 | -        | `prunsoli/yongji-backend:0.0.20`    | -                                | linux/amd64 | HTTPS 적용 과정 · HealthController 오류 해결 |
| 2025-06-17 | -        | `prunsoli/yongji-backend:0.0.19`    | -                                | linux/amd64 | HTTPS 적용 과정 · Spring Security Health 엔드포인트 추가 |
| 2025-06-17 | -        | `prunsoli/yongji-backend:0.0.18`    | -                                | linux/amd64 | HTTPS 적용 과정 · AWS 타겟그룹 Health Check 기능 추가 |


## Version Naming Convention

- 메이저 버전: 큰 기능 변경이나 호환되지 않는 API 변경
- 마이너 버전: 이전 버전과 호환되는 기능 추가
- 패치 버전: 버그 수정

예시: `1.0.0` (메이저.마이너.패치)

## 주의사항

1. 새로운 버전을 배포할 때마다 이 문서를 업데이트해주세요.
2. 주요 변경사항은 반드시 Description과 Changes 컬럼에 기록해주세요.
3. 이미지 태그는 반드시 버전과 일치시켜주세요. 