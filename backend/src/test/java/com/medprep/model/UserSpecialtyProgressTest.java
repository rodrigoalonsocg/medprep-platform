package com.medprep.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserSpecialtyProgressTest {

    private UserSpecialtyProgress progress(int total, int correct) {
        UserSpecialtyProgress p = UserSpecialtyProgress.builder()
                .totalAttempts(total)
                .correctAttempts(correct)
                .build();
        p.recalculate();
        return p;
    }

    @Test
    void sinIntentos_esRojoYCero() {
        UserSpecialtyProgress p = progress(0, 0);
        assertThat(p.getTrafficLight()).isEqualTo(UserSpecialtyProgress.TrafficLight.ROJO);
        assertThat(p.getAccuracyPercentage().doubleValue()).isZero();
    }

    @Test
    void mayorA75_esVerde() {
        UserSpecialtyProgress p = progress(100, 76);
        assertThat(p.getTrafficLight()).isEqualTo(UserSpecialtyProgress.TrafficLight.VERDE);
        assertThat(p.getAccuracyPercentage().doubleValue()).isEqualTo(76.0);
    }

    @Test
    void exactamente75_esAmarillo() {
        UserSpecialtyProgress p = progress(100, 75);
        assertThat(p.getTrafficLight()).isEqualTo(UserSpecialtyProgress.TrafficLight.AMARILLO);
    }

    @Test
    void exactamente60_esAmarillo() {
        UserSpecialtyProgress p = progress(100, 60);
        assertThat(p.getTrafficLight()).isEqualTo(UserSpecialtyProgress.TrafficLight.AMARILLO);
    }

    @Test
    void menorA60_esRojo() {
        UserSpecialtyProgress p = progress(100, 59);
        assertThat(p.getTrafficLight()).isEqualTo(UserSpecialtyProgress.TrafficLight.ROJO);
    }
}
