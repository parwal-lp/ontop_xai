CREATE DATABASE IF NOT EXISTS `books`;
USE `books`;


CREATE TABLE `tb_affiliated_writers`(
    `wr_code` INTEGER NOT NULL,
    `wr_name` CHARACTER VARYING(100) NOT NULL
);
CREATE TABLE `tb_authors`(
    `bk_code` INTEGER NOT NULL,
    `wr_id` INTEGER NOT NULL
);
CREATE TABLE `tb_bk_gen`(
    `id_bk` INTEGER NOT NULL,
    `gen_name` CHARACTER VARYING(100) NOT NULL
);
CREATE TABLE `tb_books`(
    `bk_code` INTEGER NOT NULL,
    `bk_title` CHARACTER VARYING(100) NOT NULL,
    `bk_type` CHARACTER(1) NOT NULL
);
CREATE TABLE `tb_edition`(
    `ed_code` INTEGER NOT NULL,
    `ed_type` CHARACTER(1) NOT NULL,
    `pub_date` DATE NOT NULL,
    `n_edt` INTEGER NOT NULL,
    `editor` INTEGER NOT NULL,
    `bk_id` INTEGER NOT NULL
);
CREATE TABLE `tb_editor`(
    `ed_code` INTEGER NOT NULL,
    `ed_name` CHARACTER VARYING(100) NOT NULL
);
CREATE TABLE `tb_emerge_authors`(
    `bk_code` INTEGER NOT NULL,
    `wr_id` INTEGER NOT NULL
);
CREATE TABLE `tb_on_prob_wr`(
    `wr_code` INTEGER NOT NULL,
    `wr_name` CHARACTER VARYING(100) NOT NULL
);

ALTER TABLE `tb_affiliated_writers` ADD CONSTRAINT `aff_wr_pk` PRIMARY KEY(`wr_code`);
ALTER TABLE `tb_books` ADD CONSTRAINT `bk_pk` PRIMARY KEY(`bk_code`);
ALTER TABLE `tb_edition` ADD CONSTRAINT `edition_pk` PRIMARY KEY(`ed_code`);
ALTER TABLE `tb_editor` ADD CONSTRAINT `ed_pk` PRIMARY KEY(`ed_code`);
ALTER TABLE `tb_on_prob_wr` ADD CONSTRAINT `pr_pk` PRIMARY KEY(`wr_code`);
ALTER TABLE `tb_emerge_authors` ADD CONSTRAINT `pk_emerge_authors` PRIMARY KEY(`bk_code`, `wr_id`);
ALTER TABLE `tb_authors` ADD CONSTRAINT `pk_au` PRIMARY KEY(`bk_code`, `wr_id`);
ALTER TABLE `tb_bk_gen` ADD CONSTRAINT `pk_gen` PRIMARY KEY(`id_bk`, `gen_name`);
ALTER TABLE `tb_authors` ADD CONSTRAINT `fk_affiliated_writes_book` FOREIGN KEY(`wr_id`) REFERENCES `tb_affiliated_writers`(`wr_code`);
ALTER TABLE `tb_authors` ADD CONSTRAINT `fk_written_books` FOREIGN KEY(`bk_code`) REFERENCES `tb_books`(`bk_code`);
ALTER TABLE `tb_bk_gen` ADD CONSTRAINT `fk_bk_gen` FOREIGN KEY(`id_bk`) REFERENCES `tb_books`(`bk_code`);
ALTER TABLE `tb_edition` ADD CONSTRAINT `fk_book_has_edition` FOREIGN KEY(`bk_id`) REFERENCES `tb_books`(`bk_code`);
ALTER TABLE `tb_edition` ADD CONSTRAINT `fk_edition_has_editor` FOREIGN KEY(`editor`) REFERENCES `tb_editor`(`ed_code`);
ALTER TABLE `tb_emerge_authors` ADD CONSTRAINT `fk_emerge_writes_book` FOREIGN KEY(`wr_id`) REFERENCES `tb_on_prob_wr`(`wr_code`);
ALTER TABLE `tb_emerge_authors` ADD CONSTRAINT `fk_written_book2` FOREIGN KEY(`bk_code`) REFERENCES `tb_books`(`bk_code`);

INSERT INTO `tb_affiliated_writers` VALUES
(23, 'AJ Scudiere'),
(25, 'Anne Rainey'),
(27, 'Barbara Delinsky'),
(34, 'Chas Wienke'),
(43, 'D.C. Ford'),
(45, 'D. E. Knobbe'),
(47, 'David Cogswell'),
(76, 'Douglas Clegg'),
(78, 'Iris Johanesen'),
(98, 'Jan Groft'),
(101, 'Jeff Havens'),
(102, 'Kate Pearce'),
(123, 'L.C. Higgs'),
(127, 'Melissa Mayhue'),
(134, 'Mike Green'),
(145, 'S. C. Carr'),
(156, 'Shirley Tallman'),
(167, 'Stacy Choen'),
(178, 'Susan Lyons'),
(189, 'Tim Davys'),
(234, 'Tracy Richardson'),
(245, 'William Boyd');
INSERT INTO `tb_books` VALUES
(1, 'Resonance', 'P'),
(2, 'As we Grieve', 'P'),
(3, 'Runaway Storm', 'P'),
(4, 'Neverland', 'P'),
(5, 'Eight Days to Live', 'P'),
(6, 'Scandal on Rincon Hill', 'P'),
(7, 'Amberville', 'P'),
(8, 'Some Like it Rough', 'P'),
(9, 'Zinn for Beginners', 'P'),
(10, 'Here Burns My Candle', 'P'),
(11, 'How to get fired', 'P'),
(12, 'The Twelve Little Hitlers', 'P'),
(13, 'The story of Eight the sparrow', 'P'),
(14, 'The social impact of Christina Oats songs', 'P'),
(15, 'Engineering analysis of Mazzinga', 'P'),
(16, 'Ordinary Thunderstorms', 'A'),
(17, 'A Highlander''s Homecoming', 'A'),
(18, 'Indian Summer', 'A'),
(19, 'A Dark Circus', 'A'),
(20, 'City of Stars', 'A'),
(21, 'Not My Daughter', 'E'),
(22, 'The Last Train From Paris', 'E'),
(23, 'Our Boomer Years', 'E'),
(24, 'Path of Thunder', 'E');
INSERT INTO `tb_on_prob_wr` VALUES
(267, 'Peter Griffin'),
(278, 'Homer Simpson'),
(289, 'Jon Stewart');
INSERT INTO `tb_editor` VALUES
(12, 'Paul Golden'),
(21, 'Pat Red'),
(23, 'Simon Frost'),
(32, 'Melody Albert'),
(34, 'Valerio Nin'),
(45, 'Victoria Rolls'),
(54, 'Karl Forman'),
(65, 'Fill Luckett'),
(76, 'Eric Jonnes'),
(87, 'Bill Sugar'),
(98, 'Bill Green');
INSERT INTO `tb_emerge_authors` VALUES
(14, 267),
(14, 278),
(15, 289);
INSERT INTO `tb_edition` VALUES
(10, 'X', DATE '2000-09-23', 1, 34, 24),
(12, 'E', DATE '2010-02-18', 1, 76, 1),
(21, 'E', DATE '2000-02-12', 1, 76, 2),
(23, 'S', DATE '2004-01-02', 1, 98, 3),
(32, 'S', DATE '2009-12-04', 1, 98, 4),
(34, 'E', DATE '2000-07-06', 1, 23, 5),
(39, 'X', DATE '2007-02-03', 2, 32, 20),
(40, 'X', DATE '2005-03-01', 1, 32, 21),
(43, 'X', DATE '2001-05-14', 1, 23, 6),
(45, 'S', DATE '2005-05-05', 1, 34, 7),
(50, 'X', DATE '2001-12-03', 1, 87, 22),
(54, 'X', DATE '2008-09-11', 1, 54, 8),
(56, 'S', DATE '2005-02-07', 1, 12, 9),
(65, 'E', DATE '2007-05-09', 1, 32, 10),
(67, 'X', DATE '2004-11-03', 1, 87, 11),
(70, 'X', DATE '2009-03-11', 1, 65, 23),
(73, 'X', DATE '2002-04-01', 1, 21, 17),
(74, 'X', DATE '2003-11-03', 1, 87, 18),
(76, 'X', DATE '2003-12-06', 1, 65, 12),
(78, 'S', DATE '2004-05-03', 1, 21, 15),
(82, 'X', DATE '2000-11-09', 1, 45, 16),
(87, 'S', DATE '2007-05-09', 2, 34, 7),
(89, 'S', DATE '2010-05-01', 2, 87, 2),
(90, 'E', DATE '2006-05-09', 2, 23, 5),
(91, 'X', DATE '2009-04-18', 3, 12, 6),
(92, 'E', DATE '2003-01-12', 2, 12, 6),
(98, 'E', DATE '2010-02-01', 2, 32, 10),
(99, 'X', DATE '2006-04-01', 1, 23, 19);
INSERT INTO `tb_authors` VALUES
(1, 23),
(2, 98),
(3, 45),
(4, 76),
(5, 78),
(6, 156),
(7, 189),
(1, 189),
(8, 102),
(8, 178),
(8, 25),
(9, 47),
(10, 123),
(2, 123),
(11, 101),
(11, 145),
(11, 43),
(16, 245),
(17, 127),
(18, 234),
(19, 76),
(20, 78),
(21, 27),
(22, 167),
(23, 34),
(24, 134);
INSERT INTO `tb_bk_gen` VALUES
(3, 'Fiction'),
(4, 'Horror'),
(5, 'Mystery'),
(6, 'Mystery'),
(7, 'Fantasy'),
(8, 'Romance'),
(9, 'Biographies'),
(9, 'History'),
(9, 'Politics'),
(10, 'Historical'),
(10, 'Novels'),
(11, 'Self Help'),
(12, 'Horror'),
(12, 'Humor'),
(12, 'Fiction'),
(12, 'Fantasy'),
(13, 'Fantasy'),
(13, 'Horror'),
(14, 'Cultural'),
(14, 'Music'),
(15, 'Science'),
(16, 'Mystery'),
(17, 'Romance'),
(18, 'Children'),
(19, 'Horror'),
(20, 'Horror'),
(21, 'Romance'),
(22, 'Fiction'),
(23, 'History'),
(24, 'Fiction  ');