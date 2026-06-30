const { sequelize } = require('../config/database');
const User = require('./User');
const InstagramAccount = require('./InstagramAccount');
const Post = require('./Post');
const ScheduledPost = require('./ScheduledPost');

// users 1 ── N instagram_accounts
User.hasMany(InstagramAccount, { foreignKey: 'userId', onDelete: 'CASCADE' });
InstagramAccount.belongsTo(User, { foreignKey: 'userId' });

// users 1 ── N posts
User.hasMany(Post, { foreignKey: 'userId', onDelete: 'CASCADE' });
Post.belongsTo(User, { foreignKey: 'userId' });

// instagram_accounts 1 ── N posts
InstagramAccount.hasMany(Post, { foreignKey: 'instagramAccountId' });
Post.belongsTo(InstagramAccount, { foreignKey: 'instagramAccountId' });

// posts 1 ── 0/1 scheduled_posts
Post.hasOne(ScheduledPost, { foreignKey: 'postId', onDelete: 'CASCADE' });
ScheduledPost.belongsTo(Post, { foreignKey: 'postId' });

module.exports = {
  sequelize,
  User,
  InstagramAccount,
  Post,
  ScheduledPost,
};
