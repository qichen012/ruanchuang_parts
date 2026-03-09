-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema Learning_DB
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema Learning_DB
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `Learning_DB` ;
USE `Learning_DB` ;

-- -----------------------------------------------------
-- Table `Learning_DB`.`user_information`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`user_information` (
  `id` INT NOT NULL,
  `name` VARCHAR(45) NULL,
  `gender` ENUM('male', 'female') NULL,
  `age` INT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `Learning_DB`.`source_documents`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`source_documents` (
  `id` INT NOT NULL,
  `user_id` INT NULL,
  `file_name` VARCHAR(45) NULL,
  `file_path` VARCHAR(45) NULL,
  `upload_date` DATE NOT NULL,
  `processed_status` ENUM('Pending', 'Done', 'Failed') NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `users.id_idx` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_sourcedocuments_userinformation`
    FOREIGN KEY (`user_id`)
    REFERENCES `Learning_DB`.`user_information` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `Learning_DB`.`daily_briefs`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`daily_briefs` (
  `id` INT NOT NULL,
  `user_id` INT NULL,
  `target_date` DATE NULL,
  `posterior_insight` VARCHAR(45) NULL,
  `created_at` DATETIME NULL,
  `new_review_date` DATE NULL,
  `reciew_stage` INT NULL,
  `User_reflect` VARCHAR(45) NULL,
  PRIMARY KEY (`id`),
  INDEX `users.id_idx` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_dailybriefs_userinformation`
    FOREIGN KEY (`user_id`)
    REFERENCES `Learning_DB`.`user_information` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `Learning_DB`.`review_logs`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`review_logs` (
  `id` INT NOT NULL,
  `user_id` INT NULL,
  `brief_id` INT NULL,
  `review_at` DATETIME NULL,
  `feynman_score` INT NULL,
  PRIMARY KEY (`id`),
  INDEX `users.id_idx` (`user_id` ASC) VISIBLE,
  INDEX `daily_briefs.id_idx` (`brief_id` ASC) VISIBLE,
  CONSTRAINT `fk_reviewlogs_userinformation`
    FOREIGN KEY (`user_id`)
    REFERENCES `user`.`information` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_reviewlogs_dailybriefs`
    FOREIGN KEY (`brief_id`)
    REFERENCES `Learning_DB`.`daily_briefs` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `Learning_DB`.`elite_idea_cards`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`elite_idea_cards` (
  `id` INT NOT NULL,
  `daily_bried_id` INT NULL,
  `origin_concept` VARCHAR(45) NULL,
  `meta_idea_name` VARCHAR(45) NULL,
  `meta_explanation` VARCHAR(45) NULL,
  `create_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  INDEX `id_idx` (`daily_bried_id` ASC) VISIBLE,
  CONSTRAINT `fk_eliteideacards_dailybriefs`
    FOREIGN KEY (`daily_bried_id`)
    REFERENCES `Learning_DB`.`daily_briefs` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `Learning_DB`.`elite_idea_cases`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`elite_idea_cases` (
  `id` INT NOT NULL,
  `meta_id` INT NULL,
  `case_title` VARCHAR(45) NULL,
  `case_content` VARCHAR(45) NULL,
  `image_path` VARCHAR(45) NULL,
  `query_rewrite` TEXT(100) NULL,
  PRIMARY KEY (`id`),
  INDEX `id_idx` (`meta_id` ASC) VISIBLE,
  CONSTRAINT `fk_eliteideacases_eliteideacards`
    FOREIGN KEY (`meta_id`)
    REFERENCES `Learning_DB`.`elite_idea_cards` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `Learning_DB`.`external_resources`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`external_resources` (
  `id` INT NOT NULL,
  `card_id` INT NULL,
  `title` VARCHAR(45) NULL,
  `url` VARCHAR(45) NULL,
  `LLM_context` TEXT(100) NULL,
  `source` VARCHAR(45) NULL,
  PRIMARY KEY (`id`),
  INDEX `id_idx` (`card_id` ASC) VISIBLE,
  CONSTRAINT `fk_externalresources_eliteideacards`
    FOREIGN KEY (`card_id`)
    REFERENCES `Learning_DB`.`elite_idea_cards` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `Learning_DB`.`user_screenshots`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`user_screenshots` (
  `id` INT NOT NULL,
  `user_id` INT NULL,
  `image__path` VARCHAR(45) NULL,
  `vlm_analysis` TEXT(100) NULL,
  `upload_date` DATE NULL,
  PRIMARY KEY (`id`),
  INDEX `users.id_idx` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_userscreenshots_userinformation`
    FOREIGN KEY (`user_id`)
    REFERENCES `Learning_DB`.`user_information` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `Learning_DB`.`association_briefs`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`association_briefs` (
  `id` INT NOT NULL,
  `user_id` INT NULL,
  `type` ENUM('Auto', 'Manual') NULL,
  `content` TEXT(100) NULL,
  `notes_date` DATE NULL,
  `screenshot_date` DATE NULL,
  `created_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  INDEX `users.id_idx` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_associationbriefs_userinformation`
    FOREIGN KEY (`user_id`)
    REFERENCES `Learning_DB`.`user_information` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `Learning_DB`.`scholar_notes`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`scholar_notes` (
  `id` INT NOT NULL,
  `user_id` INT NULL,
  `notes_content` TEXT(100) NULL,
  `target_date` DATE NULL,
  `daily_brief_id` INT NULL,
  PRIMARY KEY (`id`),
  INDEX `daily_briefs.id_idx` (`daily_brief_id` ASC) VISIBLE,
  INDEX `users.id_idx` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_scholarnotes_userinformation`
    FOREIGN KEY (`user_id`)
    REFERENCES `Learning_DB`.`user_information` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_scholarnotes_dailybriefs`
    FOREIGN KEY (`daily_brief_id`)
    REFERENCES `Learning_DB`.`daily_briefs` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `Learning_DB`.`knowledge_maps`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`knowledge_maps` (
  `id` INT NOT NULL,
  `user_id` INT NULL,
  `source_doc_id` INT NULL,
  `map_json` JSON NULL,
  `created_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  INDEX `source_documents.id_idx` (`source_doc_id` ASC) VISIBLE,
  INDEX `users.id_idx` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_knowledgemaps_userinformation`
    FOREIGN KEY (`user_id`)
    REFERENCES `Learning_DB`.`user_information` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_knowledgemaps_sourcedocuments`
    FOREIGN KEY (`source_doc_id`)
    REFERENCES `Learning_DB`.`source_documents` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `Learning_DB`.`map_interaction_logs`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`map_interaction_logs` (
  `id` INT NOT NULL,
  `user_id` INT NULL,
  `source_doc_id` INT NULL,
  `node_id` VARCHAR(45) NULL,
  `user_query` TINYTEXT NULL,
  `ai_response` TEXT(100) NULL,
  `created_at` DATETIME NULL,
  `is_distilled` TINYINT NULL,
  PRIMARY KEY (`id`),
  INDEX `source_documents.id_idx` (`source_doc_id` ASC) VISIBLE,
  INDEX `users.id_idx` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_mapinteractionlogs_userinformation`
    FOREIGN KEY (`user_id`)
    REFERENCES `Learning_DB`.`user_information` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_mapinteraction_sourcedocuments`
    FOREIGN KEY (`source_doc_id`)
    REFERENCES `Learning_DB`.`source_documents` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `Learning_DB`.`map_cognitive_snapshots`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Learning_DB`.`map_cognitive_snapshots` (
  `id` INT NOT NULL,
  `user_id` INT NULL,
  `source_doc_id` INT NULL,
  `last_processed_log_id` INT NULL,
  `snapshot_content` TEXT(100) NULL,
  `path_nodes` JSON NULL,
  `version` INT NULL,
  `last_log_id` INT NULL,
  PRIMARY KEY (`id`),
  INDEX `source_documents.id_idx` (`source_doc_id` ASC) VISIBLE,
  INDEX `users.id_idx` (`user_id` ASC) VISIBLE,
  CONSTRAINT `fk_mapcognitivesnapshots_userinformation`
    FOREIGN KEY (`user_id`)
    REFERENCES `Learning_DB`.`user_information` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_mapcognitivesnapshots_sourcedocuments`
    FOREIGN KEY (`source_doc_id`)
    REFERENCES `Learning_DB`.`source_documents` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
