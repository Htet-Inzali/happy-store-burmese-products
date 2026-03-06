package com.htet.happystore.service;

import com.htet.happystore.entity.User;
import com.htet.happystore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String credential)
            throws UsernameNotFoundException {

        // Email သို့မဟုတ် Phone နဲ့ User ရှာမယ်
        User user = userRepository.findByEmail(credential)
                .orElseGet(() ->
                        userRepository.findByPhone(credential)
                                .orElseThrow(() ->
                                        new UsernameNotFoundException("User not found: " + credential)));

        // Real DB principal value ကိုသုံးမယ်
        String principal = user.getEmail() != null ? user.getEmail() : user.getPhone();

        return new org.springframework.security.core.userdetails.User(
                principal,
                user.getPassword(),
                true,  // enabled
                true,  // accountNonExpired
                true,  // credentialsNonExpired
                true,  // accountNonLocked
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                )
        );
    }
}