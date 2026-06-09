import JSEncrypt from 'jsencrypt'

export function rsaEncrypt(txt) {
    const encryptor = new JSEncrypt();
    let rawKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt69hbXjJtv0mMNEVURviXG1eDdQCgc3odChsxz1ghggQlrbFwktgCqTWY/jWVqmKXrAmYV7WboXsF+g8M9pqLwDUZfHWoaBYA/VV3HIE6IV6vegLflan4wRJe89pgUP6pUrMhJ5wWlevarccrZpnyHxmWJHj26I31hSrb2KlGp64o7Yu6h3bzNZZNnRBZN7QDaEa2wK28ntfCmRcRKnIbkHRPi2/A8Ue/c2XDnX6Tf08OoiqNm35cjHJt6APmJwszca1e5Uhfu5mM1ylywrHEVsNO4Wrg/kFSyGqLsIRlIuLghVuvYbf7QX8JInvwz0wox9gihi53RQeJ8x3SsRtvQIDAQAB";
    // 补全标准 PEM 头尾
    const publicKey = `-----BEGIN PUBLIC KEY-----
    ${rawKey}
    -----END PUBLIC KEY-----`;
    // 设置公钥
    encryptor.setPublicKey(publicKey);
    // 加密
    return encryptor.encrypt(String(txt));
}