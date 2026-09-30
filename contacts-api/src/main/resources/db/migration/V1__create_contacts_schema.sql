CREATE TABLE contact_group (
                               id BIGINT NOT NULL AUTO_INCREMENT,
                               name VARCHAR(255) NOT NULL,
                               description VARCHAR(255),
                               createdTimestamp DATETIME(6),
                               PRIMARY KEY (id),
                               UNIQUE (name)
);

CREATE TABLE contact (
                         id BIGINT NOT NULL AUTO_INCREMENT,
                         firstName VARCHAR(255) NOT NULL,
                         lastName VARCHAR(255) NOT NULL,
                         email VARCHAR(255) NOT NULL,
                         phone VARCHAR(255) NOT NULL,
                         contactGroup_id BIGINT,
                         createdTimestamp DATETIME(6),
                         PRIMARY KEY (id),
                         UNIQUE (email),
                         CONSTRAINT fk_contact_group
                             FOREIGN KEY (contactGroup_id)
                                 REFERENCES contact_group(id)
);

