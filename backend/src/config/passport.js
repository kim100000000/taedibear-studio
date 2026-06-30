const { Strategy: JwtStrategy, ExtractJwt } = require('passport-jwt');
const LocalStrategy = require('passport-local').Strategy;
const GoogleStrategy = require('passport-google-oauth20').Strategy;
const KakaoStrategy = require('passport-kakao').Strategy;
const User = require('../models/User');
const bcrypt = require('bcryptjs');

module.exports = (passport) => {
  // JWT 기반 인증 (보호된 라우트)
  passport.use(
    new JwtStrategy(
      {
        jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
        secretOrKey: process.env.JWT_SECRET,
      },
      async (payload, done) => {
        try {
          const user = await User.findByPk(payload.sub);
          if (!user) return done(null, false);
          return done(null, user);
        } catch (err) {
          return done(err, false);
        }
      }
    )
  );

  // 이메일/비밀번호 로그인
  // docs/05_API명세서.md: 존재하지 않는 이메일/비밀번호 불일치 모두 동일한 메시지로 응답 (계정 존재 여부 노출 방지)
  passport.use(
    new LocalStrategy(
      { usernameField: 'email', passwordField: 'password' },
      async (email, password, done) => {
        try {
          const genericMessage = '이메일 또는 비밀번호가 올바르지 않아요.';
          const user = await User.findOne({ where: { email } });
          if (!user || !user.passwordHash) {
            return done(null, false, { message: genericMessage });
          }

          const isMatch = await bcrypt.compare(password, user.passwordHash);
          if (!isMatch) return done(null, false, { message: genericMessage });

          return done(null, user);
        } catch (err) {
          return done(err);
        }
      }
    )
  );

  // 구글 소셜 로그인 (FR-106)
  passport.use(
    new GoogleStrategy(
      {
        clientID: process.env.GOOGLE_CLIENT_ID,
        clientSecret: process.env.GOOGLE_CLIENT_SECRET,
        callbackURL: process.env.GOOGLE_CALLBACK_URL,
      },
      async (accessToken, refreshToken, profile, done) => {
        try {
          const googleId = profile.id;
          const email = profile.emails?.[0]?.value;
          const name = profile.displayName || email?.split('@')[0] || 'Taedibear User';

          // 1) 이미 구글 ID로 연결된 계정이 있으면 그대로 로그인
          let user = await User.findOne({ where: { googleId } });
          if (user) return done(null, user);

          // 2) 같은 이메일로 가입된 계정이 있으면 구글 ID만 연결 (FR-108 신규 사용자 자동 생성과 별개로 계정 통합)
          if (email) {
            user = await User.findOne({ where: { email } });
            if (user) {
              user.googleId = googleId;
              await user.save();
              return done(null, user);
            }
          }

          // 3) 신규 사용자는 자동으로 계정 생성 (FR-108), 비밀번호 없는 소셜 전용 계정
          user = await User.create({ name, email, googleId, passwordHash: null });
          return done(null, user);
        } catch (err) {
          return done(err);
        }
      }
    )
  );

  // 카카오 소셜 로그인 (FR-106와 동일한 흐름)
  passport.use(
    new KakaoStrategy(
      {
        clientID: process.env.KAKAO_CLIENT_ID,
        callbackURL: process.env.KAKAO_CALLBACK_URL,
      },
      async (accessToken, refreshToken, profile, done) => {
        try {
          const kakaoId = String(profile.id);
          const kakaoAccount = profile._json?.kakao_account;
          // 카카오는 비즈니스 채널 연동 없이는 email scope가 제공되지 않는 경우가 많다.
          // email이 없으면 고유한 가짜 이메일을 만들어 users.email NOT NULL/UNIQUE 제약을 만족시킨다.
          const email = kakaoAccount?.email || `kakao_${kakaoId}@kakao.taedibear.local`;
          const name =
            profile.displayName || profile.username || kakaoAccount?.profile?.nickname || 'Taedibear User';

          // 1) 이미 카카오 ID로 연결된 계정이 있으면 그대로 로그인
          let user = await User.findOne({ where: { kakaoId } });
          if (user) return done(null, user);

          // 2) 실제 이메일을 받았고, 같은 이메일로 가입된 계정이 있으면 카카오 ID만 연결
          if (kakaoAccount?.email) {
            user = await User.findOne({ where: { email } });
            if (user) {
              user.kakaoId = kakaoId;
              await user.save();
              return done(null, user);
            }
          }

          // 3) 신규 사용자는 자동으로 계정 생성, 비밀번호 없는 소셜 전용 계정
          user = await User.create({ name, email, kakaoId, passwordHash: null });
          return done(null, user);
        } catch (err) {
          return done(err);
        }
      }
    )
  );
};
