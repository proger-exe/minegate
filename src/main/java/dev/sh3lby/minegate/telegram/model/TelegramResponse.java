package dev.sh3lby.minegate.telegram.model;

public class TelegramResponse {
    private Boolean ok;
    private String description;
    @com.google.gson.annotations.SerializedName("error_code")
    private Integer errorCode;

    public Boolean getOk() {
        return ok;
    }

    public void setOk(Boolean ok) {
        this.ok = ok;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }
}
