package az.ingress.mapper;

import az.ingress.dao.entity.UserEntity;
import az.ingress.model.dto.UserResponseDto;

public class UserMapper {
    public static UserResponseDto mapEntityToDto(UserEntity user) {
        return UserResponseDto.builder()
                .name(user.getName())
                .surname(user.getSurname())
                .age(user.getAge())
                .balance(user.getBalance())
                .debt(user.getDebt())
                .build();
    }
}
