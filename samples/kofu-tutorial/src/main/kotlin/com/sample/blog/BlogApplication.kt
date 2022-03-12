package com.sample.blog

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.fu.kofu.configuration
import org.springframework.fu.kofu.jdbc.DataSourceType
import org.springframework.fu.kofu.jdbc.jdbc
import org.springframework.fu.kofu.templating.mustache
import org.springframework.fu.kofu.webApplication
import org.springframework.fu.kofu.webmvc.webMvc
import org.springframework.web.servlet.function.ServerResponse

val h2 = configuration {
    jdbc(DataSourceType.Hikari){
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        driverClassName = "org.h2.Driver"
        username = "sa"
        password = ""
    }
}

val persistence = configuration {
    beans {
        bean { JdbcUserRepositoryImpl(ref()) }
        bean { JdbcArticleRepositoryImpl(ref()) }
    }
}

val createDb = configuration {
    listener<ApplicationReadyEvent> {
        JdbcHelper(ref()).apply {
            createUserTable()
            createArticleTable()
        }
    }
}

val populate = configuration {
    listener<ApplicationReadyEvent> {
        val userRepository: UserRepository = ref()
        val articleRepository: ArticleRepository = ref()

        val author = Entity.New(User.of("springluca", "Luca", "Piccinelli"))
        val persistedAuthor = userRepository.save(author)
        articleRepository.save(Entity.New(
            Article(
                title = "Reactor Bismuth is out",
                headline = "Lorem ipsum",
                content = "dolor sit amet",
                authorFn = { persistedAuthor }
            )
        ))
        articleRepository.save(
            Entity.New(
                Article(
                    title = "Reactor Aluminium has landed",
                    headline = "Lorem ipsum",
                    content = "dolor sit amet",
                    authorFn = { persistedAuthor }
                )
            )
        )
    }
}

val app = webApplication {
    enable(createDb)
    profile("dev"){
        enable(populate)
    }
    enable(persistence)
    enable(h2)
    webMvc {
        mustache{
            prefix = "classpath:/views/"
        }

        router {
            GET("/"){
                ServerResponse.ok()
                    .render("blog", mapOf("title" to "Blog"))
            }
        }
    }
}

fun main(args: Array<String>) {
    app.run(args + arrayOf("--spring.profiles.active=dev"))
}
