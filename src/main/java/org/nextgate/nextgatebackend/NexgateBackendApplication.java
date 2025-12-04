package org.nextgate.nextgatebackend;

import org.nextgate.nextgatebackend.authentication_service.entity.Roles;
import org.nextgate.nextgatebackend.authentication_service.repo.RolesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class NexgateBackendApplication implements CommandLineRunner {

    @Autowired
    private RolesRepository roleRepository;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {return builder.build();}

    public static void main(String[] args) {
        SpringApplication.run(NexgateBackendApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        createRoleIfNotExists("ROLE_SUPER_ADMIN");
        createRoleIfNotExists("ROLE_STAFF_ADMIN");
        createRoleIfNotExists("ROLE_NORMAL_USER");
    }

    private void createRoleIfNotExists(String roleName) {
        Roles existingRole = roleRepository.findByRoleName(roleName).orElse(null);

        if (existingRole == null) {
            Roles newRole = new Roles();
            newRole.setRoleName(roleName);
            roleRepository.save(newRole);
        }
    }


}
