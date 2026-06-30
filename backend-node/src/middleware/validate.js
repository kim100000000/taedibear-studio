const { validationResult } = require('express-validator');
const { fail } = require('../utils/response');

// express-validator 체인 뒤에 붙여서 검증 실패 시 400 응답
module.exports = function validate(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    return fail(res, 400, errors.array()[0].msg);
  }
  next();
};
