package com.example.datingsite.controller.form;

import com.example.datingsite.model.Profile;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Об'єкт форми створення/редагування анкети (зв'язується з th:object у шаблоні).
 */
public class ProfileForm {

    // Типографський апостроф ’: звичайний ' інтерполятор повідомлень трактує як службовий символ
    @NotBlank(message = "Вкажіть ім’я")
    @Size(max = 50, message = "Ім’я має містити не більше 50 символів")
    private String name;

    @NotNull(message = "Вкажіть вік")
    @Min(value = 18, message = "Сайт доступний лише повнолітнім (від 18 років)")
    @Max(value = 100, message = "Вкажіть реальний вік (не більше 100)")
    private Integer age;

    @Size(max = 500, message = "Відкрита інформація - не більше 500 символів")
    private String publicInfo;

    @Size(max = 200, message = "Закрита інформація - не більше 200 символів")
    private String privateInfo;

    @Size(max = 200, message = "Ключові слова - не більше 200 символів")
    private String keywords;

    public static ProfileForm from(Profile profile) {
        ProfileForm form = new ProfileForm();
        form.setName(profile.getName());
        form.setAge(profile.getAge());
        form.setPublicInfo(profile.getPublicInfo());
        form.setPrivateInfo(profile.getPrivateInfo());
        form.setKeywords(String.join(", ", profile.getKeywords()));
        return form;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPublicInfo() {
        return publicInfo;
    }

    public void setPublicInfo(String publicInfo) {
        this.publicInfo = publicInfo;
    }

    public String getPrivateInfo() {
        return privateInfo;
    }

    public void setPrivateInfo(String privateInfo) {
        this.privateInfo = privateInfo;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
}
