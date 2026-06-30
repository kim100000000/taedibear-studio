const { success, fail } = require('../utils/response');
const { User } = require('../models');

// GET /api/users/me
exports.getMe = async (req, res, next) => {
  try {
    const user = await User.findByPk(req.user.id);
    if (!user) return fail(res, 404, '사용자를 찾을 수 없어요.');

    return success(res, {
      id: user.id,
      name: user.name,
      email: user.email,
      plan: user.plan,
      created_at: user.createdAt,
    });
  } catch (err) {
    next(err);
  }
};

// PUT /api/users/me
exports.updateMe = async (req, res, next) => {
  try {
    const { name } = req.body;
    if (name === undefined) {
      return fail(res, 400, 'name은 필수예요.');
    }

    const user = await User.findByPk(req.user.id);
    if (!user) return fail(res, 404, '사용자를 찾을 수 없어요.');

    user.name = name;
    await user.save();

    return success(res, { id: user.id, name: user.name });
  } catch (err) {
    next(err);
  }
};
