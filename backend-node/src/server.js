require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const morgan = require('morgan');
const passport = require('passport');

const { sequelize } = require('./models');
require('./config/passport')(passport);

const authRoutes = require('./routes/auth.routes');
const userRoutes = require('./routes/user.routes');
const instagramRoutes = require('./routes/instagram.routes');
const postsRoutes = require('./routes/posts.routes');
const scheduledRoutes = require('./routes/scheduled.routes');

const errorHandler = require('./middleware/errorHandler');
const { startScheduledPostCron } = require('./services/cron.service');

const app = express();
const PORT = process.env.PORT || 4000;

app.use(helmet());
app.use(cors({ origin: process.env.CLIENT_URL || '*', credentials: true }));
app.use(morgan('dev'));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use(passport.initialize());

app.get('/health', (req, res) => res.json({ status: 'ok', service: 'taedibear-studio-backend' }));

app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/instagram', instagramRoutes);
app.use('/api/posts', postsRoutes);
app.use('/api/scheduled', scheduledRoutes);

app.use(errorHandler);

async function start() {
  try {
    await sequelize.authenticate();
    console.log('[DB] MySQL connection established.');

    // 개발 환경에서는 모델 기준으로 테이블 4개(users, instagram_accounts, posts,
    // scheduled_posts)를 자동 생성/동기화한다. alter: true로 모델에 새로 추가된
    // 컬럼(예: naver_id)도 기존 테이블에 자동으로 추가되도록 한다.
    // 운영 환경에서는 마이그레이션 사용 권장.
    if (process.env.NODE_ENV !== 'production') {
      await sequelize.sync({ alter: true });
      console.log('[DB] Tables synced: users, instagram_accounts, posts, scheduled_posts');
    }

    app.listen(PORT, () => console.log(`[Server] Listening on port ${PORT}`));

    // 예약 업로드(scheduled_posts) cron — 1분마다 발행 대상을 확인한다.
    startScheduledPostCron();
  } catch (err) {
    console.error('[DB] Unable to connect to MySQL:', err.message);
    process.exit(1);
  }
}

start();
