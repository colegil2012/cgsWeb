package templates

yieldUnescaped '<!DOCTYPE html>'
html {
    head {
        title(title ?: 'CGS Web - Organic Produce')
        style('''
            :root {
                --primary-green: #e8f5e9;
                --accent-green: #689f38;
                --dark-green: #33691e;
                --soft-brown: #d7ccc8;
                --earth-brown: #5d4037;
                --text-color: #2e3440;
                --white: #ffffff;
            }
            body {
                font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                margin: 0;
                background-color: var(--white);
                color: var(--text-color);
            }
            header {
                background-color: var(--primary-green);
                padding: 1rem 2rem;
                display: flex;
                justify-content: space-between;
                align-items: center;
                border-bottom: 2px solid var(--soft-brown);
                position: sticky;
                top: 0;
                z-index: 1000;
            }
            .logo {
                font-size: 1.5rem;
                font-weight: bold;
                color: var(--dark-green);
            }
            nav ul {
                list-style: none;
                padding: 0;
                margin: 0;
                display: flex;
                gap: 2rem;
            }
            nav a {
                text-decoration: none;
                color: var(--earth-brown);
                font-weight: 500;
                transition: color 0.3s;
            }
            nav a:hover {
                color: var(--accent-green);
            }
            .hero {
                background-color: var(--primary-green);
                padding: 4rem 2rem;
                text-align: center;
                border-bottom-left-radius: 50px;
                border-bottom-right-radius: 50px;
            }
            .hero h1 {
                color: var(--dark-green);
                font-size: 3rem;
            }
            .btn {
                background-color: var(--earth-brown);
                color: white;
                padding: 0.8rem 1.5rem;
                border-radius: 5px;
                text-decoration: none;
                display: inline-block;
                margin-top: 1rem;
            }
            .container {
                max-width: 1200px;
                margin: 0 auto;
                padding: 2rem;
            }
        ''')
    }
    body {
        header {
            div(class: 'logo', 'CGS Organic')
            nav {
                ul {
                    li { a(href: '/', 'Home') }
                    li { a(href: '/shop', 'Shop') }
                    li { a(href: '/about', 'About') }
                }
            }
        }
        main {
            content()
        }
    }
}
