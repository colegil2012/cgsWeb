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
                align-items: right;
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
            
            .product-grid {
                 display: grid;
                 grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
                 gap: 2rem;
                 padding: 2rem 0;
            }
            .product-card {
                 background: var(--white);
                 border: 1px solid var(--soft-brown);
                 border-radius: 12px;
                 overflow: hidden;
                 transition: transform 0.2s, box-shadow 0.2s;
            }
            .product-card:hover {
                 transform: translateY(-5px);
                 box-shadow: 0 10px 20px rgba(0,0,0,0.1);
            }
            .product-card img {
                 width: 100%;
                 height: 200px;
                 object-fit: cover;
             }
             .product-info {
                 padding: 1.5rem;
             }
             .product-info h3 { margin: 0; color: var(--dark-green); }
             .category { font-size: 0.8rem; color: var(--accent-green); text-transform: uppercase; margin-bottom: 0.5rem; }
             .description { font-size: 0.9rem; height: 3em; overflow: hidden; }
             .product-footer {
                 display: flex;
                 justify-content: space-between;
                 align-items: center;
                 margin-top: 1rem;
             }
             .price { font-weight: bold; font-size: 1.2rem; color: var(--earth-brown); }
             .btn-small {
                 background-color: var(--accent-green);
                 color: white;
                 padding: 0.5rem 1rem;
                 text-decoration: none;
                 border-radius: 4px;
                 font-size: 0.8rem;
             }
             .logout-link {
                color: #c62828 !important;
                font-weight: bold;
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
                    if (session != null && session.getAttribute('username') != null) {
                        li { a(href: '/logout', class: 'logout-link', 'Logout') }
                    }
                }
            }
        }
        main {
            content()
        }
    }
}
