const multer = require('multer');
const { EXT_BY_MIME } = require('../services/s3.service');

// docs/05_API명세서.md: JPG, PNG, WEBP만 허용, 최대 10MB
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    if (!EXT_BY_MIME[file.mimetype]) {
      return cb(new Error('UNSUPPORTED_FILE_TYPE'));
    }
    cb(null, true);
  },
});

module.exports = upload;
