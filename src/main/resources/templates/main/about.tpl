package templates.main

import com.ua.estore.cgsWeb.util.ImageUrlUtil

layout 'layout.tpl',
        title: 'CGS Web | About Our Mission',
        user: user,
        cartItems: cartItems,
        content: {
            div(class: 'about-hero') {
                h1('Rooted in Louisville, Growing for You.')
                p('Bringing the heart of the Kentucky farmers market directly to your doorstep.')
            }

            div(class: 'container') {
                section(class: 'about-section split') {
                    div(class: 'about-text') {
                        h2('Our Mission')
                        p('Celtech General Store was born from a simple idea: local goods shouldn\'t be hard to find. We provide Louisville\'s farmers, crafters, and organic producers a digital storefront to reach their neighbors without the overhead of massive shipping logistics.')
                        p('By focusing on low-distance shipping and local consolidation, we reduce our carbon footprint while keeping more money in our community.')
                    }
                    div(class: 'about-image') {
                        img(src: ImageUrlUtil.resolve('/images/site-images/about-market.jpg', imagesBaseUrl), alt: 'Local Louisville Market', class: 'floating-img')
                    }
                }

                section(class: 'about-section split reverse') {
                    div(class: 'about-text') {
                        h2('Support Local, Live Better')
                        p('From heirloom tomatoes to hand-poured candles, every item on our platform is sourced within a 50-mile radius of Louisville. We believe in transparency—you\'ll always know exactly which farm or studio your products came from.')
                        p('We want to remove the obfuscation between seller and consumer, you will know and trust where all of our products are sourced, your friends and neighbors.')
                    }
                    div(class: 'about-image') {
                        img(src: ImageUrlUtil.resolve('/images/site-images/about-produce.jpg', imagesBaseUrl), alt: 'Fresh Organic Produce', class: 'floating-img')
                    }
                }

                div(class: 'future-vision') {
                    h2('Looking Ahead')
                    div(class: 'vision-grid') {
                        div(class: 'vision-card') {
                            h4('Roadie Integration')
                            p('Integration with Roadie API to provide shipping estimates and create our small scale logistics network.')
                        }
                        div(class: 'vision-card') {
                            h4('Looking outward')
                            p('While Louisville is our home, we hope to bring this local-first marketplace model to more cities across the Bluegrass.')
                        }
                    }
                }
            }
        }