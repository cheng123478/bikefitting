import JSEncrypt from 'jsencrypt'

export function rsaEncrypt(txt) {
    const encryptor = new JSEncrypt();
    let rawKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoz7yrSeKcDMEC+sM14CvCLyOGK5k6AJDgxCVWOPI/1DNMnY4suBc31bDFTaCwXFQuMiu3j04Bb8zfmH4bAxuRTYCTC3C1dEMlHEjVPA3xBZi55n+DaFHWzt24hLmN7lpNHVNh1A4z9jfMUXNtSA/x41m1CUqBGzieztVKLhUv29vW+zAs/Z2x+p4oFbMXodaxpYhrOFdG7Y14xE3Tr3wMLZbyV6ms2id0oVjLZ3JGiQUJ25YcIXjBrc8Himorw47Qpr+csarZS6dolWKSQlheXsdxxu160eqv5CE95nVAAQ4sbKTwqDtXGpYdJBsf9pvugdUSoUrl7Ewh4kLEAH6FwIDAQAB";
    // 补全标准 PEM 头尾
    const publicKey = `-----BEGIN PUBLIC KEY-----
    ${rawKey}
    -----END PUBLIC KEY-----`;
    // 设置公钥
    encryptor.setPublicKey(publicKey);
    // 加密
    return encryptor.encrypt(String(txt));
}