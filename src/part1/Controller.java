package part1;

import part2.Architects;
import part3.Engineers;

public class Controller {
    public Controller(Role role) {
        if (role == Role.ARCHITECT) {
            Architects architects = new Architects();
        } else if (role == Role.ENGINEER) {
            Engineers engineers = new Engineers();
        }
    }
}
