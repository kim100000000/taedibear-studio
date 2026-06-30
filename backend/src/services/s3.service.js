const { S3Client, PutObjectCommand } = require('@aws-sdk/client-s3');
const { v4: uuidv4 } = require('uuid');

const s3 = new S3Client({
  region: process.env.AWS_REGION,
  credentials: {
    accessKeyId: process.env.AWS_ACCESS_KEY_ID,
    secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
  },
});

const EXT_BY_MIME = {
  'image/jpeg': 'jpg',
  'image/png': 'png',
  'image/webp': 'webp',
};

// 이미지 버퍼를 S3에 업로드하고 public URL을 반환한다.
// docs/05_API명세서.md: POST /api/posts/upload 응답 - image_url
async function uploadImage(buffer, mimetype) {
  const ext = EXT_BY_MIME[mimetype];
  const key = `images/${uuidv4()}.${ext}`;
  const bucket = process.env.AWS_S3_BUCKET;

  await s3.send(
    new PutObjectCommand({
      Bucket: bucket,
      Key: key,
      Body: buffer,
      ContentType: mimetype,
    })
  );

  return `https://${bucket}.s3.${process.env.AWS_REGION}.amazonaws.com/${key}`;
}

module.exports = { uploadImage, EXT_BY_MIME };
