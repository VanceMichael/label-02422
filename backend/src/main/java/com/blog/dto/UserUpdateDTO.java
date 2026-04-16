package com.blog.dto;

import javax.validation.constraints.Email;

public class UserUpdateDTO {
    @Email(message = "邮箱格式不正确")
    private String email;

    private String nickname;

    private String avatar;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
