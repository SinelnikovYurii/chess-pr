package part.example.authorization_service.service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import part.example.authorization_service.DTO.AuthResponse;
import part.example.authorization_service.DTO.LoginRequest;
import part.example.authorization_service.DTO.RegisterRequest;
import part.example.authorization_service.DTO.RegistrationResponse;
import part.example.authorization_service.JWT.JwtUtil;
import part.example.authorization_service.models.User;
import part.example.authorization_service.repository.UserRepository;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public ResponseEntity<?> register(RegisterRequest request) {
        System.out.println("Начало регистрации пользователя: " + request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            System.out.println("Пользователь уже существует: " + request.getUsername());
            return new ResponseEntity<>(new RegistrationResponse("Пользователь уже существует"), HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            System.out.println("Email уже используется: " + request.getEmail());
            return new ResponseEntity<>(new RegistrationResponse("Email уже используется"), HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        try {
            userRepository.save(user);
            System.out.println("Пользователь успешно зарегистрирован: " + request.getUsername());
            return new ResponseEntity<>(new RegistrationResponse("Регистрация успешна"), HttpStatus.CREATED);
        } catch (Exception e) {
            System.out.println("Ошибка при сохранении пользователя: " + e.getMessage());
            return new ResponseEntity<>(new RegistrationResponse("Ошибка регистрации"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<?> login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            String token = jwtUtil.generateToken(user);

            return new ResponseEntity<>(new AuthResponse(token), HttpStatus.OK);
        } catch (AuthenticationException e) {
            return new ResponseEntity<>("Неверный логин или пароль", HttpStatus.UNAUTHORIZED);
        }
    }
}