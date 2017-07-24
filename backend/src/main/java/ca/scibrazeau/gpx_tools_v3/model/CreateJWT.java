package ca.scibrazeau.gpx_tools_v3.model;

/**
 * Created by pssemr on 2016-11-27.
 */
public class CreateJWT {
    private String privateKey;
    private String jwtIss;

    private String publicKey;
    private String jwtHeader;
    private String jwtPayload;
    private String encodedJwt;

    public String getEncodedJwt() {
        return encodedJwt;
    }

    public void setEncodedJwt(String encodedJwt) {
        this.encodedJwt = encodedJwt;
    }

    public String getJwtPayload() {
        return jwtPayload;
    }

    public void setJwtPayload(String jwtPayload) {
        this.jwtPayload = jwtPayload;
    }

    public String getJwtHeader() {
        return jwtHeader;
    }

    public void setJwtHeader(String jwtHeader) {
        this.jwtHeader = jwtHeader;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getJwtIss() {
        return jwtIss;
    }

    public void setJwtIss(String jwtIss) {
        this.jwtIss = jwtIss;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
