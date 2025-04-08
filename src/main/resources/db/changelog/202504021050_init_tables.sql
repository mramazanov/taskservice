CREATE SCHEMA task_service;

CREATE TABLE task_service.task (
    id SERIAL PRIMARY KEY,
    title VARCHAR UNIQUE NOT NULL,
    description VARCHAR NOT NULL,
    status VARCHAR NOT NULL,
    dead_line TIMESTAMP WITH TIME ZONE NOT NULL,
    author INT NOT NULL,
    assignee INT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL ,
    updated_at TIMESTAMP WITH TIME ZONE
)