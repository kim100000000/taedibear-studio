const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/database');

// docs/04_DB설계서.md 3.3 posts
const Post = sequelize.define(
  'Post',
  {
    id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
    userId: { type: DataTypes.INTEGER, allowNull: false },
    instagramAccountId: { type: DataTypes.INTEGER, allowNull: false },
    imageUrl: { type: DataTypes.TEXT, allowNull: false },
    caption: { type: DataTypes.TEXT, allowNull: true },
    hashtags: { type: DataTypes.TEXT, allowNull: true }, // 쉼표 구분 문자열로 저장
    status: {
      type: DataTypes.ENUM('draft', 'scheduled', 'posted', 'failed'),
      defaultValue: 'draft',
    },
    instagramPostId: { type: DataTypes.STRING(100), allowNull: true },
    postedAt: { type: DataTypes.DATE, allowNull: true },
  },
  {
    tableName: 'posts',
    timestamps: true,
    updatedAt: false, // 설계서에 updated_at 없음
  }
);

module.exports = Post;
