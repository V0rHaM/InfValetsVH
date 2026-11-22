package me.v0rham.infvalets.api;

public class Message {

    enum Messages {
        SUCCESS(Api.getMessage("Messages.Errors.not-permission", false));

        String msg;

        Messages(String msg) {
            this.msg = msg;
        }
    }

    enum Errors {
        NOT_PERM(Api.getMessage("Messages.Errors.not-permission", false));

        String msgErr;

        Errors(String msgErr) {
            this.msgErr = msgErr;
        }
    }
}
