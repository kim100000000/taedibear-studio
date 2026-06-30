const passport = require('passport');

// JWT로 보호된 라우트에 사용
const requireAuth = passport.authenticate('jwt', { session: false });

module.exports = { requireAuth };
