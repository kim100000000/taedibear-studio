const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/database');

// docs/04_DB설계서.md 3.4 scheduled_posts
const ScheduledPost = sequelize.define(
  'ScheduledPost',
  {
    id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
    postId: { type: DataTypes.INTEGER, allowNull: false },
    scheduledAt: { type: DataTypes.DATE, allowNull: false },
    status: {
      type: DataTypes.ENUM('pending', 'done', 'failed'),
      defaultValue: 'pending',
    },
    retryCount: { type: DataTypes.INTEGER, defaultValue: 0 },
  },
  {
    tableName: 'scheduled_posts',
    timestamps: true,
    updatedAt: false, // 설계서에 updated_at 없음
  }
);

module.exports = ScheduledPost;
