package securebank.repository;

import securebank.model.User;

import java.util.Optional;

//Storage for users. The rest of the program depends on this interface, not on text files
public interface UserRepository {

    //Finds a user by username (not case-sensitive)
    Optional<User> findByUsername(String username);

    // Saves a new user
    void save(User user);

    int count();
}
