import az.ingress.dao.entity.UserEntity
import az.ingress.dao.repository.UserRepository
import az.ingress.model.request.SaveUserRequest
import az.ingress.service.UserService
import io.github.benas.randombeans.EnhancedRandomBuilder
import io.github.benas.randombeans.api.EnhancedRandom
import spock.lang.Specification
import az.ingress.model.enums.UserStatus;
import az.ingress.exception.UserNotFoundException
import spock.lang.Unroll


class UserServiceTest extends Specification {
    EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandom()
    UserRepository userRepository
    UserService userService

    def setup() {
        userRepository = Mock()
        userService = new UserService(userRepository)
    }

    def "Test saveUser success case"() {
        given:
        def request = random.nextObject(SaveUserRequest)

        when:
        userService.saveUser(request)

        then:
        1 * userRepository.save({
            it.name == request.name &&
                    it.surname == request.surname &&
                    it.age == request.age
        })
    }

    def "getUserById should return UserResponseDto when user is ACTIVE"() {
        given:
        def id = new Random().nextLong()
        def user = new UserEntity(id, "Ulvi", "Huseynov", 20, BigDecimal.valueOf(1000), BigDecimal.ZERO, UserStatus.ACTIVE)

        userRepository.findById(id) >> Optional.of(user)

        when:
        def result = userService.getUserById(id)

        then:
        result != null
        result.name == user.name
        result.surname == user.surname
        result.age == user.age
        result.balance == user.balance
        result.debt == user.debt
    }

    def "getUserById should throw IllegalArgumentException when user is not found"() {
        given:
        def id = new Random().nextLong()

        when:
        userRepository.findById(id) >> Optional.empty()
        userService.getUserById(id)

        then:
        thrown(IllegalArgumentException)
    }

    def "getUserById should throw IllegalStateException when user is INACTIVE"() {
        given:
        def id = new Random().nextLong()
        def user = new UserEntity(id, "Ulvi", "Huseynov", 20, BigDecimal.valueOf(500), BigDecimal.ZERO, UserStatus.INACTIVE)

        when:
        userRepository.findById(id) >> Optional.of(user)
        userService.getUserById(id)

        then:
        thrown(IllegalStateException)
    }

    def "deactivateUser should set user status to INACTIVE and save"() {
        given:
        def id = new Random().nextLong()
        def user = new UserEntity(id, "Ulvi", "Huseynov", 20, BigDecimal.valueOf(1000), BigDecimal.ZERO, UserStatus.ACTIVE)

        userRepository.findById(id) >> Optional.of(user)

        when:
        userService.deactivateUser(id)

        then:
        user.getStatus() == UserStatus.INACTIVE
        1 * userRepository.save(user)
    }


    def "deactivateUser should throw exception when user is already INACTIVE"() {
        given:
        def id = new Random().nextLong()
        def user = new UserEntity(id, "Ulvi", "Huseynov", 20, BigDecimal.valueOf(1000), BigDecimal.ZERO, UserStatus.INACTIVE)

        userRepository.findById(id) >> Optional.of(user)

        when:
        userService.deactivateUser(id)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "User is already inactive."
    }

    def "deactivateUser should throw UserNotFoundException if user does not exist"() {
        given:
        def id = new Random().nextLong()
        userRepository.findById(id) >> Optional.empty()

        when:
        userService.deactivateUser(id)

        then:
        def ex = thrown(UserNotFoundException)
        ex.message == "User not found with id: " + id
    }
    def "deposit should increase balance for active user"() {
        given:
        def id = new Random().nextLong()
        def initialBalance = BigDecimal.valueOf(1000)
        def depositAmount = BigDecimal.valueOf(311)
        def user = new UserEntity(id, "Ulvi", "Huseynov", 20, initialBalance, BigDecimal.ZERO, UserStatus.ACTIVE)

        userRepository.findById(id) >> Optional.of(user)

        when:
        userService.deposit(id, depositAmount)

        then:
        user.getBalance() == initialBalance.add(depositAmount)
        1 * userRepository.save(user)
    }

    def "deposit should throw IllegalArgumentException for non-positive amount"() {
        given:
        def id = new Random().nextLong()
        def invalidAmount = BigDecimal.valueOf(-100)

        when:
        userService.deposit(id, invalidAmount)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Deposit amount must be greater than zero."
    }

    def "deposit should throw UserNotFoundException if user does not exist"() {
        given:
        def id = new Random().nextLong()
        userRepository.findById(id) >> Optional.empty()

        when:
        userService.deposit(id, BigDecimal.valueOf(500))

        then:
        def ex = thrown(UserNotFoundException)
        ex.message == "User not found with id: " + id
    }

    def "deposit should throw IllegalStateException if user is inactive"() {
        given:
        def id = new Random().nextLong()
        def user = new UserEntity(id, "Ulvi", "Huseynov", 20, BigDecimal.valueOf(1000), BigDecimal.ZERO, UserStatus.INACTIVE)

        userRepository.findById(id) >> Optional.of(user)

        when:
        userService.deposit(id, BigDecimal.valueOf(500))

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "Cannot deposit to an inactive user."
    }
    @Unroll
    def "doPayment should successfully deduct balance when user has sufficient funds"() {
        given:
        def id = new Random().nextLong()
        def user = new UserEntity(id, "Ulvi", "Huseynov", 20, BigDecimal.valueOf(1000), BigDecimal.ZERO, UserStatus.ACTIVE)
        def paymentAmount = BigDecimal.valueOf(100)

        userRepository.findById(id) >> Optional.of(user)

        when:
        userService.doPayment(id, paymentAmount)

        then:
        user.getBalance() == BigDecimal.valueOf(900)
        1 * userRepository.save(user)
    }

    def "doPayment should throw IllegalArgumentException when amount is null or zero"() {
        given:
        def id = new Random().nextLong()

        when:
        userService.doPayment(id, null)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Payment amount must be greater than zero."

        when:
        userService.doPayment(id, BigDecimal.ZERO)

        then:
        ex = thrown(IllegalArgumentException)
        ex.message == "Payment amount must be greater than zero."
    }

    def "doPayment should throw IllegalStateException if user is INACTIVE"() {
        given:
        def id = new Random().nextLong()
        def user = new UserEntity(id, "Ulvi", "Huseynov", 20, BigDecimal.valueOf(1000), BigDecimal.ZERO, UserStatus.INACTIVE)
        def paymentAmount = BigDecimal.valueOf(100)

        userRepository.findById(id) >> Optional.of(user)

        when:
        userService.doPayment(id, paymentAmount)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "Cannot process payment for an inactive user."
    }

    def "doPayment should throw IllegalArgumentException if balance is insufficient"() {
        given:
        def id = new Random().nextLong()
        def user = new UserEntity(id, "Ulvi", "Huseynov", 20, BigDecimal.valueOf(1000), BigDecimal.ZERO, UserStatus.ACTIVE)
        def paymentAmount = BigDecimal.valueOf(2000)

        userRepository.findById(id) >> Optional.of(user)

        when:
        userService.doPayment(id, paymentAmount)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Insufficient balance for payment."
    }

    def "doPayment should throw UserNotFoundException if user does not exist"() {
        given:
        def id = new Random().nextLong()
        userRepository.findById(id) >> Optional.empty()

        when:
        userService.doPayment(id, BigDecimal.valueOf(100))

        then:
        def ex = thrown(UserNotFoundException)
        ex.message == "User not found with id: " + id
    }
}
