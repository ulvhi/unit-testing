package az.ingress.service;

import az.ingress.dao.entity.UserEntity;
import az.ingress.dao.repository.UserRepository;
import az.ingress.exception.UserNotFoundException;
import az.ingress.mapper.UserMapper;
import az.ingress.model.dto.UserResponseDto;
import az.ingress.model.enums.UserStatus;
import az.ingress.model.request.SaveUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    public void saveUser(SaveUserRequest saveUserRequest) {
        var user = UserEntity.builder()
                .name(saveUserRequest.getName())
                .surname(saveUserRequest.getSurname())
                .age(saveUserRequest.getAge())
                .balance(BigDecimal.ZERO)
                .debt(BigDecimal.ZERO)
                .build();
        userRepository.save(user);
    }
    public UserResponseDto getUserById(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("User is not active.");
        }

        return UserMapper.mapEntityToDto(user);
    }
    public void deactivateUser(Long id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new IllegalStateException("User is already inactive.");
        }

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        log.info("User with ID {} has been deactivated.", id);
    }
    public void deposit(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero.");
        }

        var user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new IllegalStateException("Cannot deposit to an inactive user.");
        }

        BigDecimal userBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;
        user.setBalance(userBalance.add(amount));

        userRepository.save(user);

        log.info("Deposit of {} completed for user with ID {}. New balance: {}", amount, id, user.getBalance());
    }

    public void doPayment(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        var user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Cannot process payment for an inactive user.");
        }

        BigDecimal userBalance = user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO;

        if (userBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance for payment.");
        }

        user.setBalance(userBalance.subtract(amount));
        userRepository.save(user);

        log.info("Payment of {} completed for user with ID {}. New balance: {}", amount, id, user.getBalance());
    }
}
