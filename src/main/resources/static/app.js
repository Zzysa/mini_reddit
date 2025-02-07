let currentUser = null;
let currentUserRole = null;

document.getElementById('loginForm').addEventListener('submit', function(event) {
    event.preventDefault();
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    fetch('/api/users/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    })
    .then(response => response.json())
    .then(data => {
        if (data.message.includes("Login successful")) {
            currentUser = username;
            currentUserRole = data.role;
            localStorage.setItem('currentUser', currentUser);
            localStorage.setItem('currentUserRole', currentUserRole);
            updateUI();
            document.getElementById('username').value = '';
            document.getElementById('password').value = '';
        } else {
            alert(data.message);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Login failed: Invalid username or password');
    });
});

document.getElementById('logoutButton').addEventListener('click', function() {
    fetch('/api/users/logout', {
        method: 'POST'
    })
    .then(() => {
        currentUser = null;
        currentUserRole = null;
        localStorage.removeItem('currentUser');
        localStorage.removeItem('currentUserRole', currentUserRole);
        updateUI();
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Logout failed');
    });
});

document.getElementById('createPostForm').addEventListener('submit', function(event) {
    event.preventDefault();
    const postContent = document.getElementById('postContent').value;

    fetch('/api/posts', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(postContent)
    })
    .then(response => response.json())
    .then(post => {
        document.getElementById('postContent').value = '';
        loadPosts();
    })
    .catch(error => console.error('Error creating post:', error));
});

function loadPosts() {
    fetch('/api/posts')
        .then(response => response.json())
        .then(posts => {
            const postsContainer = document.getElementById('postsContainer');
            postsContainer.innerHTML = '';
            posts.forEach(post => {
                const postElement = document.createElement('div');

                postElement.className = 'post';
                postElement.dataset.postId = post.id;

                const authorElement = document.createElement('strong');
                authorElement.textContent = post.author;
                authorElement.className = 'post-author';

                const contentElement = document.createElement('p');
                contentElement.textContent = post.content;
                contentElement.className = 'post-content';

                postElement.appendChild(authorElement);
                postElement.appendChild(contentElement);

                if (post.author === currentUser || localStorage.getItem("currentUserRole") === "ADMIN") {
                    const editButton = document.createElement('button');
                    editButton.textContent = 'Edit';
                    editButton.addEventListener('click', () => editPost(post.id));
                    postElement.appendChild(editButton);

                    const deleteButton = document.createElement('button');
                    deleteButton.textContent = 'Delete';
                    deleteButton.addEventListener('click', () => deletePost(post.id));
                    postElement.appendChild(deleteButton);
                }

                postsContainer.appendChild(postElement);
            });
        })
        .catch(error => console.error('Error loading posts:', error));
}

function editPost(postId) {
    const newContent = prompt("Enter new content for the post:");
    if (newContent) {
        fetch(`/api/posts/${postId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(newContent)
        })
        .then(response => response.json())
        .then(updatedPost => {
            const postElement = document.querySelector(`[data-post-id="${postId}"]`);
            if (postElement) {
                postElement.querySelector('.post-content').textContent = updatedPost.content;
            }
            loadPosts();
        })
        .catch(error => console.error('Error editing post:', error));
    }
}

function deletePost(postId) {
    if (confirm("Are you sure you want to delete this post?")) {
        fetch(`/api/posts/${postId}`, {
            method: 'DELETE'
        })
        .then(() => {
            const postElement = document.querySelector(`[data-post-id="${postId}"]`);
            if (postElement) {
                postElement.remove();
            }
            loadPosts();
        })
        .catch(error => console.error('Error deleting post:', error));
    }
}


function redirectToLogin() {
    document.getElementById('loginSection').style.display = 'block';
    document.getElementById('userSection').style.display = 'none';
}

document.addEventListener('DOMContentLoaded', () => {
    currentUser = localStorage.getItem('currentUser');

    if (!currentUser) {
        redirectToLogin();
        return;
    }

    fetch('/api/users/current')
        .then(response => {
            if (!response.ok) {
                if (response.status === 401) {
                     redirectToLogin();
                     return null;
                }
                throw new Error('User session invalid');
            }

            return response.text().then(text => {
                if (!text) {
                    console.log("Empty or invalid user data from server, skipping...");
                    return;
                }
                return JSON.parse(text);
            });
        })
        .then(user => {
            if (user && user.username) {
                currentUser = user.username;
                localStorage.setItem('currentUser', currentUser);
                updateUI();
                loadPosts();
            } else {
                throw new Error('Invalid user data');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            localStorage.removeItem('currentUser');
            localStorage.removeItem('currentUserRole');
            redirectToLogin();
        });
});

function updateUI() {
    if (currentUser) {
        document.getElementById('loginSection').style.display = 'none';
        document.getElementById('userSection').style.display = 'block';
        document.getElementById('loggedInUser').textContent = currentUser;
        loadPosts();
        const resultsContainer = document.getElementById('searchResults');
        resultsContainer.innerHTML = '';
        const searchContainer= document.getElementById('searchExample');
        searchContainer.value = '';
    } else {
        redirectToLogin();
    }
}

document.getElementById('searchForm').addEventListener('submit', function(event) {
    event.preventDefault();

    const example = document.getElementById('searchExample').value;

    if (example.trim() === '') {
        alert('Please enter a search example');
        return;
    }

    fetch(`/api/posts/search?example=${encodeURIComponent(example)}`)
        .then(response => response.json())
        .then(posts => {
            const resultsContainer = document.getElementById('searchResults');
            resultsContainer.innerHTML = '';

            if (posts.length === 0) {
                resultsContainer.innerHTML = '<p>No posts found</p>';
            } else {
                posts.forEach(post => {
                    const postElement = document.createElement('div');
                    postElement.className = 'post';
                    postElement.dataset.postId = post.id;

                    const authorElement = document.createElement('strong');
                    authorElement.textContent = post.author;
                    authorElement.className = 'post-author';

                    const contentElement = document.createElement('p');
                    contentElement.textContent = post.content;
                    contentElement.className = 'post-content';

                    postElement.appendChild(authorElement);
                    postElement.appendChild(contentElement);

                    if (post.author === currentUser || localStorage.getItem("currentUserRole") === "ADMIN") {
                        const editButton = document.createElement('button');
                        editButton.textContent = 'Edit';
                        editButton.addEventListener('click', () => editPost(post.id));
                        postElement.appendChild(editButton);

                        const deleteButton = document.createElement('button');
                        deleteButton.textContent = 'Delete';
                        deleteButton.addEventListener('click', () => deletePost(post.id));
                        postElement.appendChild(deleteButton);
                    }

                    resultsContainer.appendChild(postElement);
                });
            }
        })
        .catch(error => {
            console.error('Error fetching search results:', error);
            alert('An error occurred while searching');
        });
});

document.getElementById('createAccountButton').addEventListener('click', function() {
    const username = prompt("Enter new username:");
    const password = prompt("Enter new password:");
    if (username && password) {
        fetch('/api/users/create', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        })
        .then(response => response.text())
        .then(data => {
            alert(data);
        })
        .catch(error => console.error('Error creating user:', error));
    }
});

document.getElementById('deleteAccountButton').addEventListener('click', function() {
    if (confirm("Are you sure you want to delete your account?")) {
        fetch('/api/users/delete', {
            method: 'DELETE'
        })
        .then(response => response.text())
        .then(data => {
            alert(data);
            localStorage.removeItem('currentUser');
            redirectToLogin();
        })
        .catch(error => console.error('Error deleting account:', error));
    }
});
