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
    public UserDetails loadUserByUsername(String credential) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(credential)
                .orElseGet(() ->
                        userRepository.findByPhone(credential)
                                .orElseThrow(() ->
                                        new UsernameNotFoundException("User not found: " + credential)));

        String principal = user.getEmail() != null ? user.getEmail() : user.getPhone();

        return new org.springframework.security.core.userdetails.User(
                principal,
                user.getPassword(),
                user.isActive(),  // 🌟 User အကောင့်ကို ပိတ်ထားလျှင် Login ဝင်ခွင့် မပြုတော့ပါ
                true,
                true,
                true,
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                )
        );
    }
}