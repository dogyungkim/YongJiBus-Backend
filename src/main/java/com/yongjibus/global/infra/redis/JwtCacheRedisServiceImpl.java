package com.yongjibus.global.infra.redis;
//package com.yongjibus.global.redis;
//
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import com.yongjibus.global.jwt.JwtCacheService;
//
//import lombok.RequiredArgsConstructor;
//
//@Service
//@RequiredArgsConstructor
//public class JwtCacheRedisServiceImpl implements JwtCacheService {
//
//    private final RedisTemplate<String, String> redisTemplate;
//
//    @Override
//    public void setRefreshToken(String email, String refreshToken) {
//        redisTemplate.opsForValue().set(email, refreshToken);
//    }
//
//    @Override
//    public String getRefreshToken(String email) {
//        return redisTemplate.opsForValue().get(email);
//    }
//
//    @Override
//    public void deleteRefreshToken(String email) {
//        redisTemplate.delete(email);
//    }
//}