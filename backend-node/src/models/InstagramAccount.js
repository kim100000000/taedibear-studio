const { DataTypes } = require('sequelize');
const { sequelize } = require('../config/database');

// docs/04_DB설계서.md 3.2 instagram_accounts
const InstagramAccount = sequelize.define(
  'InstagramAccount',
  {
    id: { type: DataTypes.INTEGER, primaryKey: true, autoIncrement: true },
    userId: { type: DataTypes.INTEGER, allowNull: false },
    instagramUserId: { type: DataTypes.STRING(100), allowNull: false },
    username: { type: DataTypes.STRING(100), allowNull: true },
    accessToken: { type: DataTypes.TEXT, allowNull: false },
    tokenExpiresAt: { type: DataTypes.DATE, allowNull: true },
    connectedAt: { type: DataTypes.DATE, defaultValue: DataTypes.NOW },
  },
  {
    tableName: 'instagram_accounts',
    timestamps: false,
  }
);

module.exports = InstagramAccount;
