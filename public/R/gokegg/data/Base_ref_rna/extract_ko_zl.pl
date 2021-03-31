#!usr/bin/perl -w

use strict;

die "usage:perl $0 <diff gene list> <pathway> \n" unless @ARGV == 2;

open A,$ARGV[0];

open B,$ARGV[1];

my %h;

while(<A>){
    chomp;my @a=split;
    $h{$a[0]}=1;
}
close A;
my $head1=<B>;my $head2=<B>;my $head3=<B>;my $head4=<B>;    my $head5=<B>;
print "$head1$head2$head3$head4$head5";

while(<B>){
    chomp;next if(/None/);next if(/^\s+/);
    my @a=split /\t+/;next if(!defined $a[0]);
    #my @b=split /|/,$a[1];
    if(exists $h{$a[0]}){
        my @b=split /\|/,$a[1];
        print "$a[0]\t$b[0]\n";
    }
}
close B;
