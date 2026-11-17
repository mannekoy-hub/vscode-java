import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

class Song {
    String title;
    String artist;

    public Song(String title, String artist) {
        this.title = title;
        this.artist = artist;
    }
}

public class SoundSystemKiosk extends JFrame {

    private static final String ADMIN_PASSWORD = "admin123";

    private List<Song> songs = new ArrayList<>();
    private CardLayout cardLayout;
    private JPanel mainContainer;

    private JTextField titleInput;
    private JTextField artistInput;
    private JLabel adminStatusLabel;

    private JTextField searchInput;
    private DefaultListModel<String> listModel;
    private JList<String> resultsList;
    private JLabel userStatusLabel;

    private JLabel nowPlayingInfoLabel;

    public SoundSystemKiosk() {
        setTitle("Gerald and Lowella Sound System");
        setSize(550, 580);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        preloadDefaultSongs();

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        mainContainer.add(createMainMenuPage(), "MENU");
        mainContainer.add(createAdminPage(), "ADMIN");
        mainContainer.add(createUserSearchPage(), "USER");
        mainContainer.add(createNowPlayingPage(), "PLAYING");

        add(mainContainer);
        cardLayout.show(mainContainer, "MENU");
    }

    private void preloadDefaultSongs() {
        songs.add(new Song("Hotel California", "Eagles"));
        songs.add(new Song("Bohemian Rhapsody", "Queen"));
        songs.add(new Song("My Way", "Frank Sinatra"));
        songs.add(new Song("Imagine", "John Lennon"));
        songs.add(new Song("Yesterday", "The Beatles"));
        runShellSort();
    }

    private String toLowerString(String str) {
        return str == null ? "" : str.toLowerCase();
    }

    private double stringToVal(String s) {
        double val = 0;
        int limit = Math.min(s.length(), 4);
        for (int i = 0; i < limit; i++) {
            val = val * 256 + (s.charAt(i) & 0xFF);
        }
        return val;
    }

    private JPanel createMainMenuPage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBorder(new EmptyBorder(25, 30, 20, 30));
        page.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel("GERALD AND LOWELLA SOUND SYSTEM", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 19));
        titleLabel.setForeground(new Color(0x1a, 0x25, 0x2f));
        page.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(Box.createVerticalGlue());

        JLabel adminHeader = new JLabel("ADMIN");
        adminHeader.setFont(new Font("SansSerif", Font.BOLD, 14));
        adminHeader.setForeground(new Color(0x34, 0x49, 0x5e));
        adminHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(adminHeader);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        JButton btnAdminRegister = createStyledButton("  Register Music Database", new Color(0x29, 0x80, 0xb9));
        centerPanel.add(btnAdminRegister);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 18)));

        JLabel userHeader = new JLabel("USER");
        userHeader.setFont(new Font("SansSerif", Font.BOLD, 14));
        userHeader.setForeground(new Color(0x34, 0x49, 0x5e));
        userHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(userHeader);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        JButton btnUserProceed = createStyledButton("  Proceed in Choosing a Song", new Color(0x27, 0xae, 0x60));
        centerPanel.add(btnUserProceed);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 22)));

        JButton btnExitSystem = createStyledButton("  Exit System", new Color(0xc0, 0x39, 0x2b));
        centerPanel.add(btnExitSystem);

        centerPanel.add(Box.createVerticalGlue());
        page.add(centerPanel, BorderLayout.CENTER);

        JLabel locationLabel = new JLabel("LOCATION: KABACAN NORTH COTABATO", SwingConstants.CENTER);
        locationLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        locationLabel.setForeground(new Color(0x7f, 0x8c, 0x8d));
        page.add(locationLabel, BorderLayout.SOUTH);

        btnAdminRegister.addActionListener(e -> {
            JPasswordField passwordField = new JPasswordField();
            JPanel authPanel = new JPanel(new BorderLayout(5, 5));
            authPanel.add(new JLabel("Enter Admin Password to Register Music:"), BorderLayout.NORTH);
            authPanel.add(passwordField, BorderLayout.CENTER);

            int result = JOptionPane.showConfirmDialog(
                    this, authPanel, "Admin Authentication",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
            );

            if (result == JOptionPane.OK_OPTION) {
                String enteredPassword = new String(passwordField.getPassword());
                if (enteredPassword.equals(ADMIN_PASSWORD)) {
                    adminStatusLabel.setText("Status: " + songs.size() + " songs currently in database.");
                    cardLayout.show(mainContainer, "ADMIN");
                } else {
                    JOptionPane.showMessageDialog(this, "Incorrect password! Access denied.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnUserProceed.addActionListener(e -> {
            if (songs.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No song has been registered yet by the admin.", "No Songs Available", JOptionPane.WARNING_MESSAGE);
                return;
            }
            listModel.clear();
            searchInput.setText("");
            // Populate initial list view with all available songs
            for (Song s : songs) {
                listModel.addElement(s.title + " - " + s.artist);
            }
            userStatusLabel.setText("Status: Type a title to search (" + songs.size() + " songs available). Double-click to play.");
            cardLayout.show(mainContainer, "USER");
        });

        btnExitSystem.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?\nAll registered music will be deleted.", "Exit System", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                songs.clear();
                JOptionPane.showMessageDialog(this, "Thank you for choosing Gerald and Lowella Sound System.", "Exit System", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            }
        });

        return page;
    }

    private JPanel createAdminPage() {
        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setBorder(new EmptyBorder(25, 25, 20, 25));
        page.setBackground(Color.WHITE);

        JLabel header = new JLabel("ADMIN PANEL - REGISTER MUSIC", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 17));
        header.setForeground(new Color(0x29, 0x80, 0xb9));
        page.add(header, BorderLayout.NORTH);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel titleLabel = new JLabel("Song Title:");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(titleLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        titleInput = new JTextField();
        titleInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        titleInput.setFont(new Font("SansSerif", Font.PLAIN, 14));
        titleInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(titleInput);
        formPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel artistLabel = new JLabel("Artist Name:");
        artistLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        artistLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(artistLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        artistInput = new JTextField();
        artistInput.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        artistInput.setFont(new Font("SansSerif", Font.PLAIN, 14));
        artistInput.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(artistInput);
        formPanel.add(Box.createRigidArea(new Dimension(0, 18)));

        JButton addSongBtn = createStyledButton("+ Add Song to Database", new Color(0x29, 0x80, 0xb9));
        addSongBtn.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(addSongBtn);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JButton sortBtn = createStyledButton("Sort Dataset (Shell Sort)", new Color(0x27, 0xae, 0x60));
        sortBtn.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(sortBtn);
        formPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        adminStatusLabel = new JLabel("Status: " + songs.size() + " pre-loaded songs ready.");
        adminStatusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        adminStatusLabel.setForeground(new Color(0x7f, 0x8c, 0x8d));
        adminStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(adminStatusLabel);

        formPanel.add(Box.createVerticalGlue());
        page.add(formPanel, BorderLayout.CENTER);

        JButton backBtn = new JButton("<- Back to Main Menu");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        backBtn.setPreferredSize(new Dimension(0, 38));
        page.add(backBtn, BorderLayout.SOUTH);

        addSongBtn.addActionListener(e -> onAddSongClicked());
        sortBtn.addActionListener(e -> onSortClicked());
        backBtn.addActionListener(e -> cardLayout.show(mainContainer, "MENU"));

        return page;
    }

    private JPanel createUserSearchPage() {
        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setBorder(new EmptyBorder(20, 25, 15, 25));
        page.setBackground(Color.WHITE);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(Color.WHITE);

        JLabel header = new JLabel("GERALD AND LOWELLA SOUND SYSTEM (USER)");
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setForeground(new Color(0x27, 0xae, 0x60));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(header);
        topPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        JLabel subHeader = new JLabel("SONG SELECTION KIOSK");
        subHeader.setFont(new Font("SansSerif", Font.BOLD, 15));
        subHeader.setForeground(new Color(0x2c, 0x3e, 0x50));
        subHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(subHeader);
        topPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(Color.WHITE);
        searchBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        searchBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchInput = new JTextField();
        searchInput.setFont(new Font("SansSerif", Font.PLAIN, 14));
        searchBar.add(searchInput, BorderLayout.CENTER);

        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(new Color(0xe6, 0x7e, 0x22));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        searchBtn.setFocusPainted(false);
        searchBar.add(searchBtn, BorderLayout.EAST);

        topPanel.add(searchBar);
        page.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBackground(Color.WHITE);

        JLabel resultsLabel = new JLabel("Search Results & Autocomplete:");
        resultsLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        resultsLabel.setForeground(new Color(0x34, 0x49, 0x5e));
        centerPanel.add(resultsLabel, BorderLayout.NORTH);

        listModel = new DefaultListModel<>();
        resultsList = new JList<>(listModel);
        resultsList.setFont(new Font("SansSerif", Font.PLAIN, 13));
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        centerPanel.add(new JScrollPane(resultsList), BorderLayout.CENTER);

        userStatusLabel = new JLabel("Status: Type a title to search. Double-click a song to select.");
        userStatusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        userStatusLabel.setForeground(new Color(0x7f, 0x8c, 0x8d));
        centerPanel.add(userStatusLabel, BorderLayout.SOUTH);

        page.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setBackground(Color.WHITE);

        JButton backBtn = new JButton("<- Back to Main Menu");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        backBtn.setPreferredSize(new Dimension(0, 36));
        bottomPanel.add(backBtn, BorderLayout.NORTH);

        JLabel locationLabel = new JLabel("LOCATION: KABACAN NORTH COTABATO", SwingConstants.CENTER);
        locationLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        locationLabel.setForeground(new Color(0x7f, 0x8c, 0x8d));
        bottomPanel.add(locationLabel, BorderLayout.SOUTH);

        page.add(bottomPanel, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> onSearchClicked());
        searchInput.addActionListener(e -> onSearchClicked());

        resultsList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = resultsList.getSelectedValue();
                    if (selected != null) {
                        onSongSelected(selected);
                    }
                }
            }
        });

        backBtn.addActionListener(e -> cardLayout.show(mainContainer, "MENU"));

        return page;
    }

    private JPanel createNowPlayingPage() {
        JPanel page = new JPanel(new BorderLayout(10, 15));
        page.setBorder(new EmptyBorder(30, 30, 20, 30));
        page.setBackground(Color.WHITE);

        JLabel header = new JLabel("NOW PLAYING / QUEUED", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.setForeground(new Color(0xe6, 0x7e, 0x22));
        page.add(header, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        nowPlayingInfoLabel = new JLabel("Loading song details...", SwingConstants.CENTER);
        nowPlayingInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        nowPlayingInfoLabel.setOpaque(true);
        nowPlayingInfoLabel.setBackground(new Color(0xf8, 0xf9, 0xfa));
        nowPlayingInfoLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xdc, 0xdd, 0xe1)),
                new EmptyBorder(25, 20, 25, 20)
        ));
        nowPlayingInfoLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        nowPlayingInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(nowPlayingInfoLabel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        JButton cancelBtn = createStyledButton("Cancel Music", new Color(0xc0, 0x39, 0x2b));
        cancelBtn.setHorizontalAlignment(SwingConstants.CENTER);
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(cancelBtn);

        centerPanel.add(Box.createVerticalGlue());
        page.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setBackground(Color.WHITE);

        JButton backBtn = new JButton("<- Back to Search");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        backBtn.setPreferredSize(new Dimension(0, 36));
        bottomPanel.add(backBtn, BorderLayout.NORTH);

        JLabel locationLabel = new JLabel("LOCATION: KABACAN NORTH COTABATO", SwingConstants.CENTER);
        locationLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        locationLabel.setForeground(new Color(0x7f, 0x8c, 0x8d));
        bottomPanel.add(locationLabel, BorderLayout.SOUTH);

        page.add(bottomPanel, BorderLayout.SOUTH);

        cancelBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Playback stopped and song removed from queue.", "Music Cancelled", JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(mainContainer, "USER");
        });

        backBtn.addActionListener(e -> cardLayout.show(mainContainer, "USER"));

        return page;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        return btn;
    }

    private void runShellSort() {
        int n = songs.size();
        for (int gap = n / 2; gap > 0; gap /= 2) {
            for (int i = gap; i < n; i++) {
                Song temp = songs.get(i);
                int j;
                for (j = i; j >= gap && toLowerString(songs.get(j - gap).title).compareTo(toLowerString(temp.title)) > 0; j -= gap) {
                    songs.set(j, songs.get(j - gap));
                }
                songs.set(j, temp);
            }
        }
    }

    private int interpolationSearch(String target) {
        int low = 0;
        int high = songs.size() - 1;

        String targetLower = toLowerString(target);
        double targetVal = stringToVal(targetLower);

        while (low <= high) {
            String lowTitle = toLowerString(songs.get(low).title);
            String highTitle = toLowerString(songs.get(high).title);

            if (targetLower.compareTo(lowTitle) < 0 || targetLower.compareTo(highTitle) > 0) break;

            double lowVal = stringToVal(lowTitle);
            double highVal = stringToVal(highTitle);

            if (highVal == lowVal) {
                if (lowTitle.equals(targetLower)) return low;
                break;
            }

            int pos = low + (int) ((double) (high - low) / (highVal - lowVal) * (targetVal - lowVal));
            if (pos < low) pos = low;
            if (pos > high) pos = high;

            String midTitle = toLowerString(songs.get(pos).title);
            if (midTitle.equals(targetLower)) return pos;
            if (midTitle.compareTo(targetLower) < 0) low = pos + 1;
            else high = pos - 1;
        }
        return -1;
    }

    private void onAddSongClicked() {
        String qTitle = titleInput.getText().trim();
        String qArtist = artistInput.getText().trim();

        if (qTitle.isEmpty() || qArtist.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both song title and artist name.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        songs.add(new Song(qTitle, qArtist));
        runShellSort();
        
        titleInput.setText("");
        artistInput.setText("");
        adminStatusLabel.setText("Status: Song added successfully. Total songs: " + songs.size());
        JOptionPane.showMessageDialog(this, "Song registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onSortClicked() {
        runShellSort();
        adminStatusLabel.setText("Status: Database sorted alphabetically using Shell Sort.");
        JOptionPane.showMessageDialog(this, "Database sorted successfully!", "Sorted", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onSearchClicked() {
        String query = searchInput.getText().trim();
        listModel.clear();

        if (query.isEmpty()) {
            for (Song s : songs) {
                listModel.addElement(s.title + " - " + s.artist);
            }
            userStatusLabel.setText("Status: Showing all " + songs.size() + " songs.");
            return;
        }

        // Check using interpolation search first
        int exactIndex = interpolationSearch(query);
        if (exactIndex != -1) {
            Song s = songs.get(exactIndex);
            listModel.addElement(s.title + " - " + s.artist);
        }

        // Also check for partial/substring matches (title or artist)
        for (Song s : songs) {
            if (toLowerString(s.title).contains(toLowerString(query)) || 
                toLowerString(s.artist).contains(toLowerString(query))) {
                String item = s.title + " - " + s.artist;
                if (!listModel.contains(item)) {
                    listModel.addElement(item);
                }
            }
        }

        if (listModel.isEmpty()) {
            userStatusLabel.setText("Status: No matching songs found for \"" + query + "\".");
        } else {
            userStatusLabel.setText("Status: Found " + listModel.getSize() + " matching song(s). Double-click to play.");
        }
    }

    private void onSongSelected(String selected) {
        nowPlayingInfoLabel.setText("<html><center><b>NOW PLAYING:</b><br><br><font size='5' color='#2980b9'>" + selected + "</font><br><br><i>Gerald and Lowella Sound System - Kabacan North Cotabato</i></center></html>");
        cardLayout.show(mainContainer, "PLAYING");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SoundSystemKiosk().setVisible(true);
        });
    }
}