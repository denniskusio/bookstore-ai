CREATE TABLE book (
    id          UUID          PRIMARY KEY,
    isbn        VARCHAR(13)   NOT NULL,
    title       VARCHAR(255)  NOT NULL,
    author      VARCHAR(255)  NOT NULL,
    description TEXT,
    price       NUMERIC(10,2) NOT NULL,
    stock       INTEGER       NOT NULL,
    version     BIGINT        NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,

    -- The database is the last line of defence; Bean Validation rejects bad input earlier.
    CONSTRAINT uq_book_isbn UNIQUE (isbn),
    CONSTRAINT ck_book_price_non_negative CHECK (price >= 0),
    CONSTRAINT ck_book_stock_non_negative CHECK (stock >= 0)
);
