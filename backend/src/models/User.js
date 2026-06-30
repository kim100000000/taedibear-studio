const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/database');

// docs/04_DB설계서.md 3.1 users
const User = sequelize.define(
  'User',
  {
    id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
    name: { type: DataTypes.STRING(100), allowNull: false },
    email: { type: DataTypes.STRING(255), allowNull: false, unique: true },
    passwordHash: { type: DataTypes.STRING(255), allowNull: true }, // 소셜 로그인 시 NULL
    googleId: { type: DataTypes.STRING(100), allowNull: true },
    kakaoId: { type: DataTypes.STRING(100), allowNull: true },
    naverId: { type: DataTypes.STRING(100), allowNull: true },
    plan: { type: DataTypes.ENUM('free', 'pro'), defaultValue: 'free' },
  },
  {
    tableName: 'users',
    timestamps: true, // created_at, updated_at
  }
);

module.exports = User;
